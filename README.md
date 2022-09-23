# Edge Android Simple Tunnel
This android app demonstrates how to open a tunnel and display an RTSP video stream using Nabto Edge in an extremely simplified way. See MainActivity.java for the Nabto specific code.

Note that this is not at all production ready code and is purely for understanding the basics of Nabto Edge. For production, access control must be included - ie client/device pairing and token exchange. This has all been omitted from this app.

# Building and running
Open the project in Android Studio and run the app as you would any other.

# Device (camera) application

On the device (camera) side, use the [Simple Tunnel](https://github.com/nabto/nabto-embedded-sdk/tree/master/examples/simple_tunnel) example application. It supports the simplified scenario of this app, ie with all access control removed. For production scenarios that require access control (pairing and token exchange), use the complete [TCP Tunnel Device](https://github.com/nabto/nabto-embedded-sdk/tree/master/apps/tcp_tunnel_device) application.
