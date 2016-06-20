<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jstl/core_rt" prefix="c" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Home</title>
</head>
<body>
Spring jetty websocket demo
<div id="sse">
    <a href="javascript:wsTest()">Run WebSocket</a>
</div>
<script>
    function wsTest() {
        var ws = new WebSocket("ws://localhost:8090/handler");
        ws.onopen = function () {
            console.log("ws is open")
            ws.send("message from client")
        };
        ws.onmessage = function (evt) {
            console.log(evt.data);
        };
        ws.onclose = function () {
            console.log("ws is close");
        };
    }
</script>
</body>
</html>