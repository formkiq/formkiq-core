const AWS = require('aws-sdk');

const parameterStore = new AWS.SSM()

module.exports.handler = async(event, context) => {

  console.log('Received event:', JSON.stringify(event, null, 2));
  
  var environment = process.env.APP_ENVIRONMENT;

  const param = await exports.getParameter('/formkiq/' + environment + '/console/Url');
  const triggerSource = event.triggerSource;
  const consoleurl = param.Parameter.Value;
  console.log("console url: " + consoleurl);

  if (triggerSource == 'CustomMessage_AdminCreateUser') {
    sendCreateUserEmail(event, consoleurl);
  } else if (triggerSource == 'CustomMessage_ForgotPassword') {
    sendForgotPasswordEmail(event, consoleurl);
  }

  console.log("sending email");
  console.log("Subject: " + event.response.emailSubject);
  console.log("sending email: " + event.response.emailMessage);
  return event;
};

exports.getParameter = function(param) {
  return new Promise((res, rej) => {
    parameterStore.getParameter({
      Name: param
    }, (err, data) => {
      if (err) {
        return rej(err)
      }
      return res(data)
    })
  });
}

function sendCreateUserEmail(event, consoleurl) {

  const email = event.userName;

  var text = "Hello " + email + ",<br /><br />";
  text += "Welcome to FormKiQ Stacks.<br /><br />"
  text += "FormKiQ Stacks gives you full control of your organization's documents, including storage, search, data extraction, and workflows.<br />"
  text += "All in YOUR AWS cloud.<br /><br />";
  text += "Log into your FormKiQ Stacks account<br />Console Url: " + consoleurl + "<br />Username: {username}<br />Temporary Password: {####}";

  event.response.emailSubject = "Welcome to FormKiQ Stacks";
  event.response.emailMessage = text;
}

function sendForgotPasswordEmail(event, consoleurl) {

  const userStatus = event.request.userAttributes['cognito:user_status'];
  
  if (userStatus == "CONFIRMED") {
    const email = event.userName;
    var code = event.request.codeParameter;
    var username = encodeURIComponent(event.request.userAttributes.email);

    var text = "Hello " + email + ",<br /><br />";
    text += "A password reset request has been made for your account.";
    text += "To Reset your password, please click on following link and if your browser does not open it, please copy and paste it in your browser’s address bar.<br /><br />";
    text += consoleurl + "/resetPassword?code=" + code + "&user_name=" + username + "<br /><br />";
    text += "Regards<br />FormKiQ Team";

    event.response.emailSubject = "FormKiQ Stacks - Lost Password";
    event.response.emailMessage = text;
  }
}