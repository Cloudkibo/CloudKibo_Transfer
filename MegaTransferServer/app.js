var app = require('express')();
var server = require('http').createServer(app);
var socketio = require('socket.io').listen(server);
var roomName = "MegaTransfer";

server.listen(80);

app.get('/', function (req, res) {
  res.send('Hello, Welcome to MegaTransfer. Please use android and not browser. You stupid.');
});

socketio.on('connection', function (socket) {


	var id = (Math.random().toString(10) + '0000000000000000000').substr(2, 4);

	socket.username = id;
	socket.join(roomName);
	socket.emit('id', id);
	
	console.log("client connected: "+id);
	
	socket.on('foo', function(message){
		console.log(message);
	});

	socket.on('message', function (message) {

	console.log("message from client: "+ JSON.stringify(message));

	var clients = findClientsSocket(roomName);


	var socketid = '';

      for(var i in clients){
        if(clients[i].username == message.to){
          socketid = clients[i].id;
        }
      }


      var msgToSend = message.msg;

      if(typeof msgToSend != 'object')
        msgToSend = JSON.parse(msgToSend);

    socketio.to(socketid).emit('message', msgToSend);

	});

function findClientsSocket(roomId, namespace) {
    var res = []
      , ns = socketio.of(namespace ||"/");    // the default namespace is "/"

    if (ns) {
      for (var id in ns.connected) {
        if(roomId) {
          var index = ns.connected[id].rooms.indexOf(roomId) ;
          if(index !== -1) {
            res.push(ns.connected[id]);
          }
        } else {
          res.push(ns.connected[id]);
        }
      }
    }

    return res;
  }	
		
});