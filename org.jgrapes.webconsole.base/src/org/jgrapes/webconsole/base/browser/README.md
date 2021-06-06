# Overview

The core of a JGrapes WebConsole on the client side is represented
by a singleton of type {@link Console}. The singleton is automatically
created and exported as {@link theConsole}.

WebConsole implementations must provide a class that extends
{@link Renderer} and create an instance when the main page of the
SPA is loaded. The connection to the server is established by calling
{@link Console.init}, usually from a "readystatechange" listener.