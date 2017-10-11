"""Module with functions and coroutines to handle events.

The events are HTTP requests, websockets messages, tags read
by a RFID reader, server startup and server shutdown.
"""

import base64
import binascii
import datetime as dt
import functools
import itertools
from json import JSONDecodeError

import aiohttp
import aiohttp_jinja2
import async_timeout
from aiohttp import web
#from bson import json_util as json
import bson

import asyncio
from typing import Iterable, Sequence, Set


@aiohttp_jinja2.template('index.html')
async def index(request):
    """Respond with the index page."""
    return {}

async def websocket(request):
    """Establish a websocket connection with a client."""
    ws = web.WebSocketResponse()
    await ws.prepare(request)
    app = request.app

    # Save client socket
    sockets = app['sockets']
    sockets.append(ws)

    this_client_iter = (ws,)
    #await send_status_to_clients(this_client_iter, app['reading_tags'])

    if app['previous_tags']:
        tags_msg = messages.new_tags(tuple(app['previous_tags']),
                                     app['previous_containers'])
        await send_to_clients(this_client_iter, tags_msg)

    async for msg in ws:
        if msg.tp == aiohttp.MsgType.text:
            print('Received unknown message: {!r}'.format(msg.data))

        elif msg.tp == aiohttp.MsgType.error:
            print('ws connection closed with exception %s' %
                  ws.exception())

    sockets.remove(ws)
    print('WS connection closed')
    return ws

async def startup(app):
    """Server startup routine."""
    #await start_continuous_read(app)
    #await send_status_to_clients(app['sockets'], app['reading_tags'])
    if not app['reading_tags']:
        # In case couldn't start reading tags, try again later
        reset_session_timer(app)
    #init_tags_queue_consumer(app)
    #init_intransit_task(app)


async def shutdown(app):
    """Server shutdown routine."""
    await stop_continuous_read(app)
    #await send_status_to_clients(app['sockets'], app['reading_tags'])

async def end_session(app):
    """Finalize the reading session and start a new one."""
    
def cancel_session_timer(app):
    """Cancel the session timer."""
    try:
        app['session_timer'].cancel()
    except (AttributeError, KeyError):
        pass

def end_session_cb(app):
    """Setup a task to end the reading session and start a new one."""
    app['loop'].create_task(end_session(app))

def reset_session_timer(app, seconds=1.0):
    """Set a timer to end the reading session and start a new one."""
    cancel_session_timer(app)
    app['session_timer'] = app['loop'].call_later(seconds, end_session_cb, app)

def tags_subscription_cb(app, tags_report):
    """Receive tags read from RFID reader."""
    epcid_list = tuple(tag_data_pair[0] for tag_data_pair in tags_report)
    app['tags_queue'].put_nowait(epcid_list)

async def print_aiohttp_resp(resp):
    """Helper coroutine for printing aiohttp responses."""
    print('Response:', await resp.text())

async def post_alerts(request):
    """Receive tags through HTTP Post."""
    msg = await request.text()
    print('Received Alerts:', msg)
	
    #epcid_list = bson.loads('[{"foo": [1, 2]}, {"bar": {"hello": "world"}}, {"code": {"$scope": {}, "$code": "function x() { return 1; }"}}, {"bin": {"$type": "80", "$binary": "AQIDBA=="}}]')
	
    # For debug
    #print('Received list of EPCIDs through http:', epcid_list)

    #await receive_epcids(request.app, epcid_list)
	
    return web.Response(text='{"Success":true,"msg":"Success saving"}',
						content_type="application/json")

