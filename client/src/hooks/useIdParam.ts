import { useParams } from 'react-router-dom'
import { unshorten } from '../services/short-id'

interface IdRouteParams {
  id: string
}

const useIdParam = () => {
  const { id } = useParams<IdRouteParams>()
  if (id === undefined) {
    throw new Error("Path parameter 'id' not found")
  }
  return unshorten(id)
}

export default useIdParam
