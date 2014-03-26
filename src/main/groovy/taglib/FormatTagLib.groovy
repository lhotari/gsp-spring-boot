package taglib

import grails.gsp.TagLib

import java.text.SimpleDateFormat

@TagLib
class FormatTagLib {
	def dateFormat = { attrs, body ->
		out << new SimpleDateFormat(attrs.format).format(attrs.date)
	}
}