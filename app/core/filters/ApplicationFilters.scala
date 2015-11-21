package core.filters

import javax.inject.Inject

import play.api.http.HttpFilters
import play.filters.gzip.GzipFilter

/**
  * Common filters to apply across application.
  */
class ApplicationFilters @Inject() (gzipFilter: GzipFilter, requestLoggingFilter: RequestLoggingFilter) extends HttpFilters {
  def filters = Seq(gzipFilter, requestLoggingFilter)
}
