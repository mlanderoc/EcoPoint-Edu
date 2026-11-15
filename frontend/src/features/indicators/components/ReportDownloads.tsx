/**
 * "Exportar Excel" and "Descargar PDF" buttons of the dashboard (Spec 006,
 * T512; R-530, R-531). The file is downloaded with the session cookie and
 * saved through a temporary object URL.
 *
 * @author craulgomez
 */

import { useState } from 'react'
import { FileSpreadsheet, FileText } from 'lucide-react'
import { AlertBanner, Button } from '@/components/ui'
import { ApiError } from '@/services/apiClient'
import { ReportFormat, indicatorService, reportFileName } from '../services/indicatorService'

export interface ReportDownloadsProps {
  studyId: number
}

/** Saves a downloaded file in the browser. */
export function saveFile(file: Blob, fileName: string) {
  const url = window.URL.createObjectURL(file)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

export function ReportDownloads({ studyId }: ReportDownloadsProps) {
  const [downloading, setDownloading] = useState<ReportFormat | null>(null)
  const [error, setError] = useState<string | null>(null)

  const download = async (format: ReportFormat) => {
    setError(null)
    try {
      setDownloading(format)
      saveFile(await indicatorService.report(studyId, format), reportFileName(studyId, format))
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo descargar el archivo.')
    } finally {
      setDownloading(null)
    }
  }

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-col sm:flex-row gap-2" data-testid="report-downloads">
        <Button variant="outline" className="min-h-[48px]" onClick={() => download('excel')}
          isLoading={downloading === 'excel'} disabled={downloading !== null}>
          <FileSpreadsheet className="w-5 h-5 mr-2" aria-hidden="true" />
          {downloading === 'excel' ? 'Generando Excel...' : 'Exportar Excel'}
        </Button>
        <Button variant="outline" className="min-h-[48px]" onClick={() => download('pdf')}
          isLoading={downloading === 'pdf'} disabled={downloading !== null}>
          <FileText className="w-5 h-5 mr-2" aria-hidden="true" />
          {downloading === 'pdf' ? 'Generando PDF...' : 'Descargar PDF'}
        </Button>
      </div>
      {error && <AlertBanner variant="danger" message={error} onDismiss={() => setError(null)} />}
    </div>
  )
}
