# Reactive CQRS Prototype

Sample implementation of Mobimeo Backend Engineering Challenge, 
that more or less abstracts over initially specified task, in order to show public transport tracking, that is:
- scalable
- resilient

just by design 

So this implementation handles "delay" as a constantly changing value.  


## HOW TO
start an app
```
sbt run
```
make a sample request

```
curl -X POST \
  http://localhost:8081/tracklines \
  -H 'Content-Type: application/json' \
  -d '{
	"coordinates": {"x":1, "y":2},
	"timestamp": 1552856551274
}'
```

expected response looks like that:
```
{
    "ref": "ws://localhost:8082/1x2_1552856551274"
}
```


Here we separateed Write from Read
To Read data we connect to to `ws://localhost:8082/1x2_1552856551274` for example with [wsc](https://github.com/danielstjules/wsc)
like:
```
 $ wsc ws://localhost:8082/1x2_1552856551274
   Connected to ws://localhost:8082/1x2_1552856551274
   < {
     "line" : {
       "id" : "200"
     },
     "stop" : {
       "id" : 1
     },
     "scheduled_time" : {
       "value" : 1552857606519
     },
     "expected_time" : {
       "value" : 1552857126519
     }
   }
   < ...

```

we(client) start getting updates over relevant public transport lines for point/coordinate we are interested in.

for every new `POST /tracklines` we will get new `ref` and a new stream of scheduled/expected times for every relevant line.



