import { useRef, useState } from 'react'
import { Box, IconButton } from '@mui/material'
import DragIndicatorIcon from '@mui/icons-material/DragIndicator'
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp'
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown'

/** Returns a copy of the list with the item at `from` moved to position `to`. */
export function moveItem<T>(list: T[], from: number, to: number): T[] {
  if (from === to || from < 0 || to < 0 || from >= list.length || to >= list.length) return list
  const copy = list.slice()
  const [item] = copy.splice(from, 1)
  copy.splice(to, 0, item)
  return copy
}

/**
 * Drag-and-drop for table rows plus up / down buttons (the buttons also work on a phone, where dragging does not).
 * Use rowProps(i) on the <TableRow> and <RowMoveCell index=... count=... /> inside it.
 */
export function useRowReorder(onMove: (from: number, to: number) => void) {
  const dragFrom = useRef<number | null>(null)
  const [overIndex, setOverIndex] = useState<number | null>(null)

  const rowProps = (i: number) => ({
    onDragOver: (e: React.DragEvent) => {
      if (dragFrom.current === null) return
      e.preventDefault()
      if (overIndex !== i) setOverIndex(i)
    },
    onDrop: (e: React.DragEvent) => {
      e.preventDefault()
      if (dragFrom.current !== null && dragFrom.current !== i) onMove(dragFrom.current, i)
      dragFrom.current = null
      setOverIndex(null)
    },
    sx: overIndex === i && dragFrom.current !== null && dragFrom.current !== i
      ? { boxShadow: 'inset 0 3px 0 0 #1976d2' }
      : undefined,
  })

  const handleProps = (i: number) => ({
    draggable: true,
    onDragStart: (e: React.DragEvent<HTMLElement>) => {
      dragFrom.current = i
      e.dataTransfer.effectAllowed = 'move'
      e.dataTransfer.setData('text/plain', String(i))
      const row = (e.currentTarget as HTMLElement).closest('tr')
      if (row) e.dataTransfer.setDragImage(row, 20, 20)
    },
    onDragEnd: () => {
      dragFrom.current = null
      setOverIndex(null)
    },
  })

  return { rowProps, handleProps, onMove }
}

interface MoveProps {
  index: number
  count: number
  handleProps: ReturnType<ReturnType<typeof useRowReorder>['handleProps']>
  onMove: (from: number, to: number) => void
}

/** Drag handle with up and down buttons. */
export function RowMover({ index, count, handleProps, onMove }: MoveProps) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', whiteSpace: 'nowrap' }}>
      <Box {...handleProps} title="Drag to move this line up or down"
        sx={{ cursor: 'grab', display: 'flex', color: 'text.secondary', '&:active': { cursor: 'grabbing' } }}>
        <DragIndicatorIcon fontSize="small" />
      </Box>
      <IconButton size="small" aria-label="Move up" disabled={index === 0} onClick={() => onMove(index, index - 1)}>
        <KeyboardArrowUpIcon fontSize="small" />
      </IconButton>
      <IconButton size="small" aria-label="Move down" disabled={index >= count - 1} onClick={() => onMove(index, index + 1)}>
        <KeyboardArrowDownIcon fontSize="small" />
      </IconButton>
    </Box>
  )
}
