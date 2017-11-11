import sys
import asyncio
from aiohttp import web

from server import UnitecDemoServer

# Free access to Windows Firewall
#C:\Users\20006030\AppData\Local\Android\sdk\emulator\qemu\windows-x86_64

# Configuration
config = {
    'host': '0.0.0.0',
    'port': 8888,
}

loop = asyncio.get_event_loop()
loop.set_debug(True)

while not loop.is_closed():
    try:
        UnitecDemoServer(loop).run(**config)
    except KeyboardInterrupt:
        loop.close()