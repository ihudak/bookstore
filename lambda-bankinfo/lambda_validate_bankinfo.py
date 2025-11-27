"""
    Simple AWS Lambda with a sample web request function instrumented with OneAgent
    Called from the Dynapay BankinfoController - returns 200 or 400 based on the bank name is in a predefined list or not.
    Gets the payload through an API Gateway.
    Sample payload with success: {"used_bank":"Value Banks Inc."}
    Sample payload which would result in error: {"used_bank":"Not Exist Inc."}
    RUNTIME: Python 3.11
    LAYER INFORMATION:
        (1) arn:aws:lambda:us-east-1:725887861453:layer:Dynatrace_OneAgent_1_283_0_20231201-074341_with_collector_python:1
            - See documentation https://docs.dynatrace.com/docs/shortlink/aws-lambda-extension
    TRIGGER:
        API Gateway: HTTP API with Open security

    steps.
    1. create a new function and copy-paste its body and click "Deploy"
    2. set env vars in the config (Get Data to Dynatrace -> Lambda -> ...)
    3. add an arn layer (AWS Lambda UI -> Layers)
    4. add trigger -> add API Gateway: HTTP API with Open security
    5. add env vars for troubleshooting:
        DT_LOGGING_DESTINATION=stdout
        DT_LOGGING_NODEJS_FLAGS=Exporter=true

    tests:
    -- success
    {
        "body": "{\"used_bank\": \"Right Bank.\"}"
    }
    --fail
    {
      "body": "{\"used_bank\": \"Wrong Bank.\"}"
    }
"""


import json
import logging
import base64
import os
import boto3


# Configure the logger
logger = logging.getLogger()
logger.setLevel(logging.INFO)  # Set the desired log level (DEBUG, INFO, WARNING, ERROR, CRITICAL)
logger.info('Loading function')

# Create the boto3 client once per execution environment for performance
lambda_client = boto3.client('lambda')

# Configure the second Lambda's name/alias/ARN via environment variable
SECOND_LAMBDA_NAME = os.environ.get('SECOND_LAMBDA_NAME', 'REPLACE_WITH_SECOND_LAMBDA_NAME')

def return_response(json_data, status_code):
    return {
        'statusCode': status_code,
        'body': json_data,
        'headers' : {
            'Access-Control-Allow-Headers': 'Content-Type,x-dtc',
            'Access-Control-Allow-Credentials': 'true',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'OPTIONS,POST,PUT,GET'
        }
    }

def _parse_body(event):
    if event.get("isBase64Encoded"):
        logger.warning("got base64 encoded body")
        b64encoded_event_body = event.get("body")
        event_body_bytes = base64.b64decode(b64encoded_event_body)
        return json.loads(event_body_bytes.decode('utf-8'))
    else:
        return json.loads(event.get("body"))

def _call_second_lambda(key1_value: str, bank_name: str) -> dict:
    # Build the payload your Node.js Lambda expects
    payload = {
        "key1": key1_value,                  # "value1" or "value0"
        "key2": bank_name,                  # optional, helps with logging
        "key3": "from-first-lambda"         # optional, customize if you want
    }
    out = lambda_client.invoke(
        FunctionName=SECOND_LAMBDA_NAME,
        InvocationType='RequestResponse',    # synchronous call
        Payload=json.dumps(payload).encode('utf-8')
    )
    # The Node.js Lambda returned an object with statusCode + body (stringified JSON)
    raw = out['Payload'].read()
    try:
        resp_obj = json.loads(raw.decode('utf-8')) if raw else {}
        # body is a JSON string (e.g., {"allow": true, ...})
        body_json = json.loads(resp_obj.get('body', '{}'))
        return {
            "statusCode": resp_obj.get("statusCode"),
            "body": body_json
        }
    except Exception:
        logger.exception(f"Failed to parse second Lambda response: {raw}")
        return {"statusCode": None, "body": {}}

def get_bank_info(event, context):
    
    
    allowed_bank_names = [
        "Ward Holdings",
        "Connection Financial Group",
        "Eminence Trust",
        "Goldleaf Banks",
        "His Majesty Holdings",
        "Value Banks Inc.",
        "Concorde Financial Group",
        "Obsidian Trust Corp.",
        "Pursuit Bank Inc.",
        "Right Bank."
    ]

    event_body_obj = _parse_body(event)
    bank_name = event_body_obj.get("used_bank")

    print("got bank name: " + bank_name)

    allowed = bank_name in allowed_bank_names
    key1 = "value1" if allowed else "value0"

    # Invoke second Lambda and read `allow` from its JSON body
    second = _call_second_lambda(key1, bank_name)
    allow_from_second = second["body"].get("allow")
    logger.info(f"Second Lambda returned allow={allow_from_second} for bank={bank_name} (statusCode={second['statusCode']})")

    # Keep your original behavior; include downstream info for visibility
    if allowed:
        status_code = 200
        res = f"{bank_name} bank is allowed (downstream allow={allow_from_second})"
        logger.info(f"{bank_name} is allowed")
    else:
        status_code = 400
        res = f"{bank_name} bank is NOT allowed (downstream allow={allow_from_second})"
        logger.error(f"{bank_name} is NOT allowed")

    # Optional: warn if there's a mismatch between local decision and downstream
    if allowed and allow_from_second is False:
        logger.warning("Mismatch: locally allowed but second Lambda returned allow=false")
    if not allowed and allow_from_second is True:
        logger.warning("Mismatch: locally NOT allowed but second Lambda returned allow=true")

    return return_response(res, status_code)

def lambda_handler(event, context):
    handler_log = 'lambda_handler: Received event=' + str(event) + " with event body=" + str(event.get("body"))
    logger.info(handler_log)
    return get_bank_info(event, context)
