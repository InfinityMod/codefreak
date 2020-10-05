import React from 'react'
import { Route, Switch } from 'react-router-dom'
import EmbeddedLayout from '../components/EmbeddedLayout'
import DeepLinkPage from '../pages/lti/DeepLinkPage'
import LaunchPage from '../pages/lti/LaunchPage'
import NotFoundPage from './NotFoundPage'

const LtiPage: React.FC = () => {
  return (
    <EmbeddedLayout>
      <Switch>
        <Route path={`${process.env.REACT_APP_PUBLIC_URI}/lti/deep-link`} component={DeepLinkPage} />
        <Route path={`${process.env.REACT_APP_PUBLIC_URI}//lti/launch/:id`} component={LaunchPage} />
        <Route>
          <NotFoundPage />
        </Route>
      </Switch>
    </EmbeddedLayout>
  )
}

export default LtiPage
