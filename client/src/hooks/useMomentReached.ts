import { Moment } from 'moment'
import { useCallback, useEffect, useState } from 'react'

export type NowFn = typeof Date.now

/**
 * Hook that continuously checks if date has been reached.
 * If the date is reached the return value changes to true.
 * Also allows to pass in undefined in which case the hook simply returns undefined as well.
 *
 * @param date A {Date} instance or unix timestamp in milliseconds as {number}.
 * @param nowFn A function returning the current timestamp in milliseconds
 * @param precision An interval in ms
 */
const useMomentReached = (
  date: undefined | Moment,
  nowFn: NowFn = Date.now,
  precision: number = 100
): boolean | undefined => {
  const checkDateReached = useCallback((): boolean | undefined => {
    const timestamp = date?.unix()
    return timestamp ? timestamp <= nowFn() : undefined
  }, [date, nowFn])
  const [dateReached, setDateReached] = useState<boolean | undefined>(
    checkDateReached()
  )

  useEffect(() => {
    // handle possible change of date
    const reached = checkDateReached()
    setDateReached(reached)
    if (reached === false) {
      // schedule checks if deadline has NOT been reached (and is not undefined)
      const intervalId = setInterval(() => {
        const reached = checkDateReached()
        setDateReached(reached)
        if (reached !== false) clearInterval(intervalId)
      }, precision)
      return () => clearInterval(intervalId)
    }
  }, [date, nowFn, checkDateReached, setDateReached, precision])

  return dateReached
}

export default useMomentReached
