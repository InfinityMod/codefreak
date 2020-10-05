// workaround for https://github.com/facebook/create-react-app/issues/5280

const proxy = require('http-proxy-middleware')

const proxyHost = process.env.NODE_PROXY_HOST || 'localhost'
const proxyPort = process.env.NODE_PROXY_PORT || '8080'
const proxySubSite = process.env.REACT_APP_PUBLIC_URI || ''
const proxyUrl = `${proxyHost}:${proxyPort}${proxySubSite}`

module.exports = function (app) {
  ;[`/api`, `/graphql`, `/lti/login`, `/oauth2`, `/login`].forEach(path => {
    app.use(proxy(`http://${proxyUrl}${path}`))
  })
  app.use(
    proxy(`${proxySubSite}/subscriptions`, {
      target: `ws://${proxyUrl}`,
      ws: true
    })
  )
}
