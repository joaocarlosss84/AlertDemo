"""Module with the server class definition."""

import os
import sys

import aiohttp
import aiohttp_jinja2
import jinja2
from aiohttp import web

import asyncio


import routes, handles


class UnitecDemoServer:
    """Server for portal.

    This implements the server to run the following services:
        - websockets service of containners read by RFID reader
        - web interface to visualize the containners read by the RFID reader
        - web interface to administrate the containers database
    """

    def __init__(self, loop=None):
        """Initialize PardiniServer."""
        app = web.Application(loop=loop)
        app.on_startup.append(handles.startup)
        app.on_shutdown.append(handles.shutdown)
        routes.initialize(app)

        if not loop:
            loop = asyncio.get_event_loop()
        app['loop'] = loop

        base_dir = os.path.dirname(os.path.abspath(__file__))
        app['base_dir'] = base_dir
        app['static_dir'] = base_dir + '/static/'
        app['photos_dir'] = app['static_dir'] + 'avatars/'
        app.router.add_static('/static/', app['static_dir'])
        aiohttp_jinja2.setup(app, loader=jinja2.FileSystemLoader(base_dir + '/templates/'))

        app['sockets'] = []
        app['client_session'] = aiohttp.ClientSession()
        app['previous_tags'] = set()
        app['previous_containers'] = list()
        app['session_timer'] = None
        app['reading_tags'] = False
        app['tags_queue'] = asyncio.Queue(loop=loop)
        self.app = app

    def run(self, host='127.0.0.1', port=8888, debug=False):
        """Run the server."""
        app = self.app

        loop = app['loop']

        # For debug purposes
        if debug:
            loop.create_task(get_user_command(loop, rfid._execute))

        web.run_app(app, host=host, port=port)


async def get_user_command(loop, coro):
    """Get user input and call the suplied coroutine with it.

    Coroutine that calls 'coro' with the user's input each time the
    user inserts a line on the standard input.

    For debug purposes only.
    """
    while True:
        line = await loop.run_in_executor(None, sys.stdin.readline)
        try:
            await coro(line.strip())
        except asyncio.TimeoutError:
            pass