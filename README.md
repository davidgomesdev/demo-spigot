## Essential commands

`/gamerule doDayLightCycle false`
`/gamerule doWeatherCycle false`

## Auto deploy

For auto-deploy, install PlugManX and change this part of its config:

```yaml
auto-reload:
  enabled: true
  check-every-seconds: 1
```

Then to deploy run the `deployPlugin` Gradle task with the env variable `SERVER_PLUGINS_DIRECTORY=<...>/server/plugins`

## Debugging

We can log and also attach a debugger to the server.

Info here: https://docs.papermc.io/paper/dev/debugging/
