/**
 * JS API to interact with Garabatos Backend
 */

"use strict"

// uses ES6 module notation (export statement is at the very end)
// see https://medium.freecodecamp.org/anatomy-of-js-module-systems-and-building-libraries-fadcd8dbd0e


/**
 * Returns a random int between min & max, both inclusive
 * @param {Number} min
 * @param {Number} max
 */
function randomInRange(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

/**
 * Main configuration
 */
const server = {
    baseUrl = 'localhost:8000',
    apiKey = '' + randomInRange(10000, 99999),
    apiUrl = server.baseUrl + '/' + apiKey + '/api',
    wsUrl = server.baseUrl + '/ws',
}

/**
 * WebSocket API, which only works once initialized
 */
const ws = {
		
	/**
	 * WebSocket, or null if none connected
	 */
	socket: null,
	
	/**
	 * Sends a string to the server via the websocket.
	 * @param {string} text to send 
	 * @returns nothing
	 */
	send: (text) => {
		if (ws.socket != null) {
			ws.socket.send(text);
		}
	},

	/**
	 * Default action when text is received. Replace to do something useful.
	 * @returns nothing
	 */
	receive: (text) => {
		console.log(text);
	},
	
	/**
	 * Attempts to establish communication with the specified
	 * web-socket endpoint. If successful, will call
	 * 'ws,receive(message)' for incoming messages
	 */
	initialize: (endpoint) => {
		try {
			ws.socket = new WebSocket(endpoint);
			ws.socket.onmessage = (e) => ws.receive(e.data);
			console.log("Connected to WS '" + endpoint + "'")
		} catch (e) {
			console.log("Error, connection to WS '" + endpoint + "' FAILED: ", e);
		}
	}
} 

/**
 * Actions to perform once the page is fully loaded
 */
window.addEventListener('load', () => {
    if (server.socketUrl !== false) {
		ws.initialize(server.socketUrl);
	}
});
