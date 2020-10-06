import { MenuDataItem } from '@ant-design/pro-layout/lib/typings'
import { Spin } from 'antd'
import React, { useEffect, useState } from 'react'
import {
  BrowserRouter as Router,
  Redirect,
  Route,
  Switch
} from 'react-router-dom'
import './App.less'
import Centered from './components/Centered'
import DefaultLayout from './components/DefaultLayout'
import ScrollToHash from './components/ScrollToHash'
import {
  AuthenticatedUser,
  AuthenticatedUserContext
} from './hooks/useAuthenticatedUser'
import {
  ServerTimeOffsetProvider,
  useCalculatedServerTimeOffset
} from './hooks/useServerTimeOffset'
import IdePage from './pages/IdePage'
import LoginPage from './pages/LoginPage'
import LtiPage from './pages/LtiPage'
import NotFoundPage from './pages/NotFoundPage'
import { routerConfig } from './router.config'
import {
  useGetAuthenticatedUserQuery,
  useLogoutMutation
} from './services/codefreak-api'
import { messageService } from './services/message'
import { displayName } from './services/user'
import { noop } from './services/util'

const App: React.FC<{ onUserChanged?: () => void }> = props => {
  const onUserChanged = props.onUserChanged || noop
  const [authenticatedUser, setAuthenticatedUser] = useState<
    AuthenticatedUser
  >()
  const timeOffset = useCalculatedServerTimeOffset()

  const { data: authResult, loading } = useGetAuthenticatedUserQuery({
    context: { disableGlobalErrorHandling: true },
    fetchPolicy: 'network-only'
  })

  const [logout, { data: logoutSucceeded }] = useLogoutMutation()

  useEffect(() => {
    if (authResult !== undefined) {
      setAuthenticatedUser(authResult.me)
    }
  }, [authResult])

  useEffect(() => {
    if (logoutSucceeded) {
      messageService.success('Successfully signed out. Goodbye 👋')
      setAuthenticatedUser(undefined)
      onUserChanged()
    }
  }, [logoutSucceeded, onUserChanged])

  if (loading || timeOffset === undefined) {
    return (
      <Centered>
        <Spin size="large" />
      </Centered>
    )
  }

  if (authenticatedUser === undefined) {
    const onLogin = (user: AuthenticatedUser) => {
      messageService.success(`Welcome back, ${displayName(user)}!`)
      setAuthenticatedUser(user)
      onUserChanged()
    }
    return <LoginPage onSuccessfulLogin={onLogin} />
  }

  const routes: MenuDataItem[] = []
  flattenRoutes(routerConfig.routes || [], routes)

  return (
    <ServerTimeOffsetProvider value={timeOffset}>
      <AuthenticatedUserContext.Provider value={authenticatedUser}>
        <Router basename={`${process.env.REACT_APP_PUBLIC_URI}/`}>
          <ScrollToHash />
          <Switch>
            <Route exact path={`${process.env.REACT_APP_PUBLIC_URI}/`}>
              <Redirect to={`${process.env.REACT_APP_PUBLIC_URI}/assignments/`} />
            </Route>
            <Route path={`${process.env.REACT_APP_PUBLIC_URI}/ide/:type/:id`} component={IdePage} />
            <Route path={`${process.env.REACT_APP_PUBLIC_URI}/lti`} component={LtiPage} />
            {routes.map(renderRoute(logout))}
            <DefaultLayout logout={logout}>
              <Route component={NotFoundPage} />
            </DefaultLayout>
          </Switch>
        </Router>
      </AuthenticatedUserContext.Provider>
    </ServerTimeOffsetProvider>
  )
}

const flattenRoutes = (items: MenuDataItem[], routes: MenuDataItem[]) => {
  for (const item of items) {
    const { children = [], ...itemWihtoutChildren } = item
    flattenRoutes(children, routes)
    routes.push(itemWihtoutChildren)
  }
}

const renderRoute = (logout: () => {}) => (
  item: MenuDataItem,
  index: number
): React.ReactNode => {
  const { component: Component, ...props } = item
  return (
    <Route key={index} {...props}>
      <DefaultLayout logout={logout}>
        <Component />
      </DefaultLayout>
    </Route>
  )
}

export default App
