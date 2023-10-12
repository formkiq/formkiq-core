
const AWS = require('aws-sdk');

const {
  verifierFactory,
  errors: { JwtVerificationError, JwksNoMatchingKeyError },
} = require('@southlane/cognito-jwt-verifier');

const verifierId = verifierFactory({
  region: process.env.REGION,
  userPoolId: process.env.COGNITO_USER_POOL_ID,
  appClientId: process.env.COGNITO_USER_POOL_CLIENT_ID,
  tokenType: 'id'
});

const verifierAccess = verifierFactory({
  region: process.env.REGION,
  userPoolId: process.env.COGNITO_USER_POOL_ID,
  appClientId: process.env.COGNITO_USER_POOL_CLIENT_ID,
  tokenType: 'access'
});

const api = new AWS.ApiGatewayManagementApi({
  endpoint: process.env.API_URL
});

var ddb = new AWS.DynamoDB({apiVersion: '2012-08-10'});

exports.handler = async (event) => {

  if (process.env.DEBUG == "true") { 
    console.log(event);
  }

  const route = event != null && event.requestContext != null && event.requestContext.routeKey != null 
    ? event.requestContext.routeKey
    : "";

  let obj = {"route": route};
  console.log(JSON.stringify(obj));

  switch (route) {
    case '$connect':
      return connect(event);
      break;
    case '$disconnect':
      return disconnect(event);
      break;
    default:

      if (event.Records != null) {

        let promises = [];
        event.Records.forEach(r => {
          try {
            let obj = JSON.parse(r.body);
            if (obj.siteId != null && obj.message != null) {
              promises.push(processMessages(obj.siteId, r.body));
            }
          } catch (e) {}
        });
        
        return Promise.all(promises).then(() => {
          return response(200, 'all good');
        });

      } else {
        console.log('Received unknown route:', route);
        return response(404, 'Received unknown route:' + route);
      }
	}    
}

async function processMessages(siteId, message) {
  
  var params = {
    ExpressionAttributeValues: {
      ':pk': {S: siteId + "/connections"}
    },
    KeyConditionExpression: 'PK = :pk',
    TableName: process.env.WEB_CONNECTIONS_TABLE
  };

  return ddb.query(params).promise().then((data) => {

    if (process.env.DEBUG == "true") {
      console.log("found: " + data.Items.length + " connections");
    }

    let list = [];
    data.Items.forEach(item => {

      if (process.env.DEBUG == "true") {
        console.log("sending to connection: " + item.connectionId.S);
      }

      list.push(replyToMessage(message, item.connectionId.S));
    });

    return Promise.all(list);
  });
}

async function response(statusCode, body) {
 	let response = {
    statusCode: statusCode,
    body: body
  };
    
  console.log("response: " + JSON.stringify(response));
  return response;
}

async function connect(event) {

  const connectionId = event.requestContext.connectionId;
  console.log('Connection for id: ' + connectionId);

  return verifyToken(event).then((data) => {

    return registerConnection(connectionId, data).then(() => {
      return response(200, "connected")
    });

  }).catch((err) => {
    console.log("ERR: " + err);
  	return response(400, "unable to verify token");
  });
}

async function registerConnection(connectionId, data) {
  
  let promises = [];

  const SECONDS_IN_AN_HOUR = 60 * 60;
  const secondsSinceEpoch = Math.round(Date.now() / 1000);
  const expirationTime = secondsSinceEpoch + 24 * SECONDS_IN_AN_HOUR;

  data["cognito:groups"].forEach(group => {

    var params = {
      TableName: process.env.WEB_CONNECTIONS_TABLE,
      Item: {
        'PK' : {S: group + "/connections"},
        'SK' : {S: connectionId},
        'connectionId' : {S: connectionId},
        'GSI1PK' : {S: connectionId},
        'GSI1SK' : {S: group + "/connections"},
        'TimeToLive': {N: "" + expirationTime}
      }
    };

    promises.push(ddb.putItem(params).promise());
  });

  return Promise.all(promises);
}

async function deregisterConnection(connectionId) {

  var params = {
    ExpressionAttributeValues: {
      ':pk': {S: connectionId}
    },
    KeyConditionExpression: 'GSI1PK = :pk',
    IndexName: "GSI1",
    TableName: process.env.WEB_CONNECTIONS_TABLE
  };

  return ddb.query(params).promise().then((data) => {

    let list = [];
    data.Items.forEach(i => {

      var item = {
        Key: {
          "PK": { S: i.PK.S }, 
          "SK": { S: i.SK.S }
        }, 
        TableName: process.env.WEB_CONNECTIONS_TABLE
      };

      list.push(ddb.deleteItem(item).promise());
    });

    return Promise.all(list);
  });

}

async function disconnect(event) {
  const connectionId = event.requestContext.connectionId;
  console.log('Disconnect for id: ' + connectionId);
  return deregisterConnection(connectionId).then(() => {
  	return response(200, "disconnected");
  });
}

async function verifyToken(event) {
  var authentication = event.headers.Authentication;
  if (authentication == null) {
    authentication = event.queryStringParameters.Authentication;
  }

  return verifierId.verify(authentication).catch((err) => {
    return verifierAccess.verify(authentication);
  });
}

async function replyToMessage(response, connectionId) {
  const data = { message: response }
  const params = {
    ConnectionId: connectionId,
    Data: Buffer.from(JSON.stringify(data))
  }

  return api.postToConnection(params).promise().catch((err) => {
    return Promise.resolve(err);
  });
}
