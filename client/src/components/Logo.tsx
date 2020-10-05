import React, { ImgHTMLAttributes } from 'react'

const Logo: React.FC<ImgHTMLAttributes<HTMLImageElement>> = props => {
  return (
    <img
      src={process.env.REACT_APP_PUBLIC_URI + '/codefreak-logo.svg'}
      alt="Code FREAK Logo"
      title="Code FREAK"
      {...props}
    />
  )
}

export default Logo
