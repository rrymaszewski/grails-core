package org.codehaus.groovy.grails.web.taglib

import grails.test.mixin.TestFor

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin
import org.codehaus.groovy.grails.plugins.codecs.HTML4Codec
import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib

import spock.lang.Ignore;
import spock.lang.Specification

@TestFor(ApplicationTagLib)
class ApplyCodecTagSpec extends Specification {
    GrailsApplication application
    
    def setup() {
        application = grailsApplication
        CodecsGrailsPlugin codecsMockPlugin = new CodecsGrailsPlugin()
        codecsMockPlugin.onChange.delegate = this
        [HTMLCodec, HTML4Codec].each { codecsMockPlugin.onChange([source: it]) }
    }
        
    def "applyCodec tag should apply codecs to values"() {
        when:
            def output=applyTemplate('<g:applyCodec name="html">${"<script>"}</g:applyCodec>')
        then:
            output=='&lt;script&gt;'
    }
    
    def "applyCodec tag should apply codecs to scriptlets"() {
        when:
            def output=applyTemplate('<g:applyCodec name="html"><%= "<script>" %></g:applyCodec>')
        then:
            output=='&lt;script&gt;'
    }
    
    @Ignore
    def "applyCodec tag should not re-apply codecs"() {
        when:
            def output=applyTemplate('<g:applyCodec name="html"><g:applyCodec name="html"><%= "<script>" %></g:applyCodec></g:applyCodec>')
        then:
            output=='&lt;script&gt;'
    }
}