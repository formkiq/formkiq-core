const AWS = require('aws-sdk');

module.exports.handler = async(event, context) => {

  console.log('Received event:', JSON.stringify(event));
  
  var topicArn = process.env.EMAIL_TOPIC_ARN;
  var consoleUrl = process.env.CONSOLE_URL;

  var records = [];
  event.Records.forEach(element => {
  	if (element.body != null) {
  		var msg = JSON.parse(element.body);
  		if (msg.Message != null) {
  			records.push(JSON.parse(msg.Message));
  		}
  	}
  });
  
  var ssm = new AWS.SSM();
  
  var params = [];
  records.forEach(r => {
  	params.push({
      Subject: 'A document has been created in FormKiQ',
      Message: 'A document has been created in FormKiq with ID ' + r.documentId 
      	+ '.\n\nClick the following <a href="' + consoleUrl + 'documents/' + r.documentId + '">link</a> to view the document in the console.',
      TopicArn: topicArn
  	});
  });
  
  let sns = new AWS.SNS({apiVersion: '2010-03-31'});
  
  var promises = [];
    
  params.forEach(p => {
  	promises.push(sns.publish(p).promise());
  });

  return Promise.all(promises);
};