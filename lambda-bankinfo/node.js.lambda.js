console.log('Loading function');

export const handler = async (event) => {

    console.log('value1 =', event.key1);
    console.log('value2 =', event.key2);
    console.log('value3 =', event.key3);

    console.log('event =', event);
    console.log('event.body =', event.body);

    const response = {
        statusCode: event.key1 === 'value1' ?  200 : 400,
        body: JSON.stringify({ status: event.key1 === 'value1' ? 'Success' : 'Failure', allow: event.key1 === 'value1', key1: event.key1, key2: event.key2, key3: event.key3 }),
    }

    return response;
};


// test
// {
//     "key1": "value1",
//     "key2": "value2",
//     "key3": "value3"
// }
