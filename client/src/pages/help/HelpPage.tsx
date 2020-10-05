import { PageHeaderWrapper } from '@ant-design/pro-layout'
import { Card } from 'antd'
import React from 'react'
import { Link } from 'react-router-dom'
import useHasAuthority from '../../hooks/useHasAuthority'
import BasicHelpPage from './BasicHelpPage'

const HelpPage: React.FC = () => {
  const onlyShowBasics = !useHasAuthority('ROLE_TEACHER')

  return (
    <>
      <PageHeaderWrapper />
      {onlyShowBasics ? (
        <BasicHelpPage noHeader />
      ) : (
          <>
            <Card title="General" style={{ marginBottom: 16 }}>
              <ul style={{ marginBottom: 0 }}>
                <li>
                  <Link to={`${process.env.REACT_APP_PUBLIC_URI}/help/basics`}>Basics</Link>
                </li>
              </ul>
            </Card>
            <Card title="For teachers" style={{ marginBottom: 16 }}>
              <ul style={{ marginBottom: 0 }}>
                <li>
                  <Link to={`${process.env.REACT_APP_PUBLIC_URI}/help/definitions`}>Definition Files</Link>
                </li>
              </ul>
            </Card>
          </>
        )}
    </>
  )
}

export default HelpPage
