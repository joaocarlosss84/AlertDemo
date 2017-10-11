"""Module with server's routes."""

import handles

def initialize(app):
    """Initialize the aiohttp app with the server routes."""
    # Index
    app.router.add_route('GET', '/', handles.index)

    # Index websocket
    app.router.add_route('GET', '/ws', handles.websocket)

    # Route to receive tags from handheld
    app.router.add_route('POST', '/alerts', handles.post_alerts)