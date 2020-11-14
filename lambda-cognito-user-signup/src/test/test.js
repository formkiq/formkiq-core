const expect = require('chai').expect;
const lambda = require('../index');
const sinon = require('sinon');
const fs = require('fs');
const path = require('path');

describe('event', () => {

    let ssmStub;

    beforeEach(function() {
        const result = {
            Parameter: {
                Value: "https://prod.tryformkiq.com"
            }
        };

        ssmStub = sinon.stub(lambda, 'getParameter');
        ssmStub.callsFake(function(param) {
            return new Promise((res, rej) => {
                return res(result);
            });
        });
    });

    afterEach(function() {
        ssmStub.restore();
    });

    it('Create User Email', function(done) {
        let json = fs.readFileSync(path.join(__dirname, "json/CustomMessage_AdminCreateUser.json"));
        let event = JSON.parse(json);

        lambda.handler(event, null).then((data) => {
            expect(data.response.emailSubject).to.eq("Welcome to FormKiQ Stacks");
            expect(data.response.emailMessage).to.include("Hello test@formkiq.com,");
            expect(data.response.emailMessage).to.include('Log into your FormKiQ Stacks account<br />Console Url: https://prod.tryformkiq.com');
            expect(data.response.emailMessage).to.include('Username: {username}');
            expect(data.response.emailMessage).to.include('Temporary Password: {####}');

            done();
        });
    });

    it('Create Admin User Email', function(done) {
        let json = fs.readFileSync(path.join(__dirname, "json/CustomMessage_AdminUser.json"));
        let event = JSON.parse(json);

        lambda.handler(event, null).then((data) => {
            expect(data.response.emailSubject).to.eq("Welcome to FormKiQ Stacks");
            expect(data.response.emailMessage).to.include("Hello test@formkiq.com,");
            expect(data.response.emailMessage).to.include('Log into your FormKiQ Stacks account<br />Console Url: https://prod.tryformkiq.com');
            expect(data.response.emailMessage).to.include('Username: {username}');
            expect(data.response.emailMessage).to.include('Temporary Password: {####}');

            done();
        });
    });

    it('Forgot Password Email', function(done) {
        let json = fs.readFileSync(path.join(__dirname, "json/CustomMessage_ForgotPassword.json"));
        let event = JSON.parse(json);

        lambda.handler(event, null).then((data) => {
            expect(data.response.emailSubject).to.eq("FormKiQ Stacks - Lost Password");
            expect(data.response.emailMessage).to.include("Hello test@formkiq.com,");
            expect(data.response.emailMessage).to.include('A password reset request has been made for your account.');
            expect(data.response.emailMessage).to.include('To Reset your password, please click on following link');
            expect(data.response.emailMessage).to.include('https://prod.tryformkiq.com/resetPassword?code={####}&user_name=test%40formkiq.com<br /><br />Regards<br />FormKiQ Team');

            done();
        });
    });
    
    it('Reset Required Email', function(done) {
        let json = fs.readFileSync(path.join(__dirname, "json/CustomMessage_ResetRequired.json"));
        let event = JSON.parse(json);

        lambda.handler(event, null).then((data) => {
            expect(data.response.emailSubject).to.eq(null);
            expect(data.response.emailMessage).to.eq(null);

            done();
        });
    });
});