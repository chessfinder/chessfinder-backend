package main
package main

import (
	"fmt"
	"testing"

	"github.com/aws/aws-lambda-go/events"
	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/dynamodb"
	"github.com/stretchr/testify/assert"

	"github.com/aws/aws-sdk-go/service/dynamodb/dynamodbattribute"
	"github.com/aws/aws-sdk-go/service/dynamodb/dynamodbiface"

	"net/http"

	"github.com/wiremock/go-wiremock"
)

func TestHandler_for_successfully_writing_contacts_into_the_table_and_sending_them_to_omnisend(t *testing.T) {
	awsConfig := aws.Config{
		Region:     aws.String("us-east-1"),
		Endpoint:   aws.String("http://localhost:4566"), // this is the LocalStack endpoint for all services
		DisableSSL: aws.Bool(true),
	}
	handler := Handler{
		queueUrl:         "http://localhost:4566/000000000000/Contacts.fifo",
		omnisnedUrl:      "http://0.0.0.0:18443",
		omnisnedKey:      "omnisendKey",
		contactTableName: "contacts",
		awsConfig:        &awsConfig,
	}

// 	awsSession, err := session.NewSession(handler.awsConfig)
// 	if err != nil {
// 		assert.FailNow(t, "failed to establish aws session!")
// 		return
// 	}

// 	dynamodbClient := dynamodb.New(awsSession)
// 	wiremockClient := wiremock.NewClient("http://0.0.0.0:18443")
// 	defer wiremockClient.Reset()

// 	message1 := events.SQSMessage{
// 		MessageId: "message1",
// 		Body:      `{"email":"Fbosco777@gmail.com","firstName":"JeanBosco","lastName":"Franck","countryCode":"US","state":"New Jersey"}`,
// 	}
// 	contact1 := Contact{Email: "Fbosco777@gmail.com", FirstName: "JeanBosco", LastName: "Franck", CountryCode: "US", State: "New Jersey"}
// 	contactRecord1 := ContactRecord{Email: "Fbosco777@gmail.com", FirstName: "JeanBosco", LastName: "Franck", CountryCode: "US", State: "New Jersey"}

// 	message2 := events.SQSMessage{
// 		MessageId: "message1",
// 		Body:      `{"email":"monicajoy36@gmail.com","firstName":"Monica","lastName":"Williams","countryCode":"US","state":"Florida"}`,
// 	}
// 	contact2 := Contact{Email: "monicajoy36@gmail.com", FirstName: "Monica", LastName: "Williams", CountryCode: "US", State: "Florida"}
// 	contactRecord2 := ContactRecord{Email: "monicajoy36@gmail.com", FirstName: "Monica", LastName: "Williams", CountryCode: "US", State: "Florida"}

// 	contact1Stub, err := stubOmnisendCall(handler, wiremockClient, contact1)
// 	if err != nil {
// 		assert.FailNow(t, fmt.Sprintf("%v", err.Error()))
// 	}
// 	defer wiremockClient.DeleteStubByID(contact1Stub.UUID())

// 	contact2Stub, err := stubOmnisendCall(handler, wiremockClient, contact2)
// 	if err != nil {
// 		assert.FailNow(t, fmt.Sprintf("%v", err.Error()))
// 	}
// 	defer wiremockClient.DeleteStubByID(contact2Stub.UUID())

// 	event := events.SQSEvent{
// 		Records: []events.SQSMessage{message1, message2},
// 	}

// 	expectedResponse, err := handler.Handle(&event)
// 	if err != nil {
// 		assert.FailNow(t, fmt.Sprintf("%v", err.Error()))
// 	}

// 	actualResponse := events.SQSEventResponse{BatchItemFailures: []events.SQSBatchItemFailure{}}

// 	assert.Equal(t, actualResponse, *expectedResponse, "Not all events are processed successfully!")

// 	actualContacts, err := scanContacts(dynamodbClient, handler)
// 	if err != nil {
// 		assert.FailNow(t, fmt.Sprintf("%v", err.Error()))
// 	}

// 	expectedContactsInDynamoDb := []ContactRecord{contactRecord1, contactRecord2}

// 	assert.Equal(t, actualContacts, expectedContactsInDynamoDb, fmt.Sprintf("Not all contacts are present in %v table!", handler.contactTableName))

// 	verifyContact1Stub, err := wiremockClient.Verify(contact1Stub.Request(), 1)
// 	if err != nil {
// 		assert.FailNow(t, fmt.Sprintf("%v", err.Error()))
// 	}
// 	assert.Equal(t, true, verifyContact1Stub, fmt.Sprintf("Stub of contact %v was not called!", contact1))

// 	verifyContact2Stub, err := wiremockClient.Verify(contact2Stub.Request(), 1)
// 	if err != nil {
// 		assert.FailNow(t, fmt.Sprintf("%v", err.Error()))
// 	}
// 	assert.Equal(t, true, verifyContact2Stub, fmt.Sprintf("Stub of contact %v was not called!", contact2))
// }

// func scanContacts(dynamodbClient dynamodbiface.DynamoDBAPI, handler Handler) (contacts []ContactRecord, err error) {

// 	fetchAllContactsRequest := &dynamodb.ScanInput{
// 		TableName: aws.String(handler.contactTableName),
// 	}

// 	allContactsReaponse, err := dynamodbClient.Scan(fetchAllContactsRequest)
// 	if err != nil {
// 		return
// 	}

// 	contacts = []ContactRecord{}

// 	err = dynamodbattribute.UnmarshalListOfMaps(allContactsReaponse.Items, &contacts)

// 	if err != nil {
// 		return
// 	}

// 	return
// }

// func stubOmnisendCall(handler Handler, wiremockClient *wiremock.Client, contact Contact) (*wiremock.StubRule, error) {

// 	requestBody := fmt.Sprintf(`
// 		{
// 			"identifiers": [
// 				{
// 					"channels": {
// 						"email": {
// 							"status": "subscribed"
// 						}
// 					},
// 					"id": "%v",
// 					"type": "email"
// 				}
// 			],
// 			"tags": [
// 				"new"
// 			],
// 			"firstName": "%v",
// 			"lastName": "%v",
// 			"country": "United States",
// 			"countryCode": "US",
// 			"state": "%v"
// 		}
// 	`, contact.Email, contact.FirstName, contact.LastName, contact.State)

// 	responseBody := fmt.Sprintf(
// 		`
// 			{
// 				"email": "%v",
// 				"contactID": "645a2e5906e027001293cc2b",
// 				"firstName": "%v",
// 				"lastName": "%v"
// 			}
// 	`, contact.Email, contact.FirstName, contact.LastName)

// 	stubRule := wiremock.Post(wiremock.URLPathEqualTo("/v3/contacts")).
// 		WithBodyPattern(wiremock.EqualToJson(requestBody)).
// 		WithHeader("X-API-KEY", wiremock.EqualTo(handler.omnisnedKey)).
// 		WillReturnResponse(
// 			wiremock.NewResponse().
// 				WithBody(responseBody).
// 				WithHeader("Content-Type", "application/json").
// 				WithStatus(http.StatusOK),
// 		)
// 	err := wiremockClient.StubFor(stubRule)
// 	if err != nil {
// 		return nil, err
// 	}
// 	return stubRule, nil
// }
