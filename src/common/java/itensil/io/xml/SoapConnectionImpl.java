package itensil.io.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNode;
import org.apache.axiom.om.OMText;
import org.apache.axiom.om.impl.llom.OMTextImpl;
import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.OperationClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.DispatchPhase;
import org.apache.axis2.saaj.SOAPMessageImpl;
import org.apache.axis2.saaj.util.IDGenerator;
import org.apache.axis2.saaj.util.SAAJUtil;
import org.apache.axis2.saaj.util.UnderstandAllHeadersHandler;
import org.apache.axis2.wsdl.WSDLConstants;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/*
 * This soley exists to fix SWA attachments in the javax.xml.soap.SOAPConnection API
 * 
 * 
 * ORIGINAL CODE FROM:
 *  
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


/**
 *
 */
public class SoapConnectionImpl extends SOAPConnection {

    /** Attribute which keeps track of whether this connection has been closed */
    private boolean closed = false;

    private ServiceClient serviceClient;


    /**
     * Sends the given message to the specified endpoint and blocks until it has returned the
     * response.
     *
     * @param request  the <CODE>SOAPMessage</CODE> object to be sent
     * @param endpoint an <code>Object</code> that identifies where the message should be sent. It
     *                 is required to support Objects of type <code>java.lang.String</code>,
     *                 <code>java.net.URL</code>, and when JAXM is present <code>javax.xml.messaging.URLEndpoint</code>
     * @return the <CODE>SOAPMessage</CODE> object that is the response to the message that was
     *         sent
     * @throws javax.xml.soap.SOAPException if there is a SOAP error, or this SOAPConnection is
     *                                      already closed
     */
    public SOAPMessage call(SOAPMessage request, Object endpoint) throws SOAPException {
        if (closed) {
            throw new SOAPException("SOAPConnection closed");
        }

        // initialize URL
        URL url;
        try {
            url = (endpoint instanceof URL) ? (URL)endpoint : new URL(endpoint.toString());
        } catch (MalformedURLException e) {
            throw new SOAPException(e.getMessage());
        }

        // initialize and set Options
        Options options = new Options();
        options.setTo(new EndpointReference(url.toString()));

        // initialize the Sender
        OperationClient opClient;
        try {
            serviceClient = new ServiceClient();   
            disableMustUnderstandProcessing(serviceClient.getAxisConfiguration());            
            opClient = serviceClient.createClient(ServiceClient.ANON_OUT_IN_OP);
        } catch (AxisFault e) {
            throw new SOAPException(e);
        }

        options.setProperty(Constants.Configuration.CHARACTER_SET_ENCODING, 
                            request.getProperty(SOAPMessage.CHARACTER_SET_ENCODING));

        opClient.setOptions(options);

        if (request.countAttachments() != 0) { // SOAPMessage with attachments
            opClient.getOptions().setProperty(Constants.Configuration.ENABLE_SWA,
                                              Constants.VALUE_TRUE);
            return handleSOAPMessage(request, opClient);
        } else { // simple SOAPMessage
            return handleSOAPMessage(request, opClient);
        }
    }

    /*
     * Installs UnderstandAllHeadersHandler that marks all headers as processed 
     * because MU validation should not be done for SAAJ clients.
     */
    private void disableMustUnderstandProcessing(AxisConfiguration config) {
        DispatchPhase phase;
        phase = getDispatchPhase(serviceClient.getAxisConfiguration().getInFlowPhases());
        if (phase != null) {
            phase.addHandler(new UnderstandAllHeadersHandler());
        }
        phase = getDispatchPhase(serviceClient.getAxisConfiguration().getInFaultFlowPhases());
        if (phase != null) {
            phase.addHandler(new UnderstandAllHeadersHandler());
        }
    }
    
    private static DispatchPhase getDispatchPhase(List phases) {
        Iterator iter = phases.iterator();
        while(iter.hasNext()) {
            Object phase = iter.next();
            if (phase instanceof DispatchPhase) {
                return (DispatchPhase)phase;
            }
        }
        return null;        
    }
    
    /**
     * Closes this <CODE>SOAPConnection</CODE> object.
     *
     * @throws javax.xml.soap.SOAPException if there is a SOAP error, or this SOAPConnection is
     *                                      already closed
     */
    public void close() throws SOAPException {
        if (serviceClient != null) {
            try {
                serviceClient.cleanup();
            } catch (AxisFault axisFault) {
                throw new SOAPException(axisFault.getMessage());
            }
        }
        if (closed) {
            throw new SOAPException("SOAPConnection Closed");
        }
        closed = true;
    }

    private SOAPMessage handleSOAPMessage(SOAPMessage request,
                                          OperationClient opClient) throws SOAPException {

        MessageContext requestMsgCtx = new MessageContext();
        try {
            requestMsgCtx.setEnvelope(SAAJUtil.toOMSOAPEnvelope(request.getSOAPPart().getDocumentElement()));
            final Iterator attachments = request.getAttachments();
            while (attachments.hasNext()) {
                final AttachmentPart attachment = (AttachmentPart)attachments.next();
                if (attachment.getContentId() == null ||
                        attachment.getContentId().trim().length() == 0) {
                    attachment.setContentId(IDGenerator.generateID());
                }
                requestMsgCtx.addAttachment(attachment.getContentId(), attachment.getDataHandler());
            }
            
            opClient.addMessageContext(requestMsgCtx);
            opClient.execute(true);

            MessageContext msgCtx =
                    opClient.getMessageContext(WSDLConstants.MESSAGE_LABEL_IN_VALUE);
            return getSOAPMessage(msgCtx.getEnvelope());
        } catch (Exception e) {
            throw new SOAPException(e.getMessage(), e);
        }
    }

    /**
     * This method handles the conversion of an OM SOAP Envelope to a SAAJ SOAPMessage
     *
     * @param respOMSoapEnv
     * @return the SAAJ SOAPMessage
     * @throws SOAPException If an exception occurs during this conversion
     */
    private SOAPMessage getSOAPMessage(org.apache.axiom.soap.SOAPEnvelope respOMSoapEnv)
            throws SOAPException {

        // Create the basic SOAP Message
        MessageFactory mf = new org.apache.axis2.saaj.MessageFactoryImpl();
        SOAPMessage response = mf.createMessage();
        SOAPPart sPart = response.getSOAPPart();
        javax.xml.soap.SOAPEnvelope env = sPart.getEnvelope();
        SOAPBody body = env.getBody();
        SOAPHeader header = env.getHeader();

        // Convert all header blocks
        org.apache.axiom.soap.SOAPHeader header2 = respOMSoapEnv.getHeader();
        if (header2 != null) {
            for (Iterator hbIter = header2.examineAllHeaderBlocks(); hbIter.hasNext();) {
                // Converting a single OM SOAP HeaderBlock to a SAAJ SOAP
                // HeaderBlock
                org.apache.axiom.soap.SOAPHeaderBlock hb = (org.apache.axiom.soap.SOAPHeaderBlock)
                        hbIter.next();
                final QName hbQName = hb.getQName();
                final SOAPHeaderElement headerEle = header.addHeaderElement(env.createName(hbQName
                        .getLocalPart(), hbQName.getPrefix(), hbQName.getNamespaceURI()));
                for (Iterator attribIter = hb.getAllAttributes(); attribIter.hasNext();) {
                    OMAttribute attr = (OMAttribute)attribIter.next();
                    final QName attrQName = attr.getQName();
                    headerEle.addAttribute(env.createName(attrQName.getLocalPart(), attrQName
                            .getPrefix(), attrQName.getNamespaceURI()), attr.getAttributeValue());
                }
                final String role = hb.getRole();
                if (role != null) {
                    headerEle.setActor(role);
                }
                headerEle.setMustUnderstand(hb.getMustUnderstand());

                toSAAJElement(headerEle, hb, response);
            }
        }

        // Convert the body
        toSAAJElement(body, respOMSoapEnv.getBody(), response);

        return response;
    }

    /**
     * Converts an OMNode into a SAAJ SOAPElement
     *
     * @param saajEle
     * @param omNode
     * @param saajSOAPMsg
     * @throws SOAPException If conversion fails
     */
    private void toSAAJElement(SOAPElement saajEle,
                               OMNode omNode,
                               javax.xml.soap.SOAPMessage saajSOAPMsg) throws SOAPException {

        if (omNode instanceof OMText) {
            return; // simply return since the text has already been added to saajEle
        }

        if (omNode instanceof OMElement) {
            OMElement omEle = (OMElement)omNode;
            for (Iterator childIter = omEle.getChildren(); childIter.hasNext();) {
                OMNode omChildNode = (OMNode)childIter.next();
                SOAPElement saajChildEle = null;

                if (omChildNode instanceof OMText) {
                    // check whether the omtext refers to an attachment

                    final OMText omText = (OMText)omChildNode;
                    if (omText.isOptimized()) { // is this an attachment?
                        final DataHandler datahandler = (DataHandler)omText.getDataHandler();
                        AttachmentPart attachment = saajSOAPMsg.createAttachmentPart(datahandler);
                        final String id = IDGenerator.generateID();
                        attachment.setContentId(id);
                        attachment.setContentType(datahandler.getContentType());
                        saajSOAPMsg.addAttachmentPart(attachment);

                        saajEle.addAttribute(
                                saajSOAPMsg.getSOAPPart().getEnvelope().createName("href"),
                                "cid:" + id);
                    } else {
                        saajChildEle = saajEle.addTextNode(omText.getText());
                    }
                } else if (omChildNode instanceof OMElement) {
                    OMElement omChildEle = (OMElement)omChildNode;
                    final QName omChildQName = omChildEle.getQName();
                    saajChildEle =
                            saajEle.addChildElement(omChildQName.getLocalPart(),
                                                    omChildQName.getPrefix(),
                                                    omChildQName.getNamespaceURI());
                    for (Iterator attribIter = omChildEle.getAllAttributes();
                         attribIter.hasNext();) {
                        OMAttribute attr = (OMAttribute)attribIter.next();
                        final QName attrQName = attr.getQName();
                        saajChildEle.addAttribute(saajSOAPMsg.getSOAPPart().getEnvelope().
                                createName(attrQName.getLocalPart(),
                                           attrQName.getPrefix(),
                                           attrQName.getNamespaceURI()),
                                                  attr.getAttributeValue());
                    }
                }

                // go down the tree adding child elements, till u reach a leaf(i.e. text element)
                toSAAJElement(saajChildEle, omChildNode, saajSOAPMsg);
            }
        }
    }


    /** overrided SOAPConnection's get() method */

    public SOAPMessage get(Object to) throws SOAPException {
        URL url = null;
        try {
            url = (to instanceof URL) ? (URL)to : new URL(to.toString());
        } catch (MalformedURLException e) {
            throw new SOAPException(e);
        }

        int responseCode;
        boolean isFailure = false;
        HttpURLConnection httpCon = null;
        try {
            httpCon = (HttpURLConnection)url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setDoInput(true);
            httpCon.setUseCaches(false);
            httpCon.setRequestMethod("GET");
            HttpURLConnection.setFollowRedirects(true);

            httpCon.connect();
            responseCode = httpCon.getResponseCode();
            // 500 is allowed for SOAP faults
            if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                isFailure = true;
            } else if ((responseCode / 100) != 2) {
                throw new SOAPException("Error response: (" + responseCode
                        + httpCon.getResponseMessage());
            }
        } catch (IOException e) {
            throw new SOAPException(e);
        }

        //Construct the soapmessage from http response
        SOAPMessage soapMessage = null;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try {
                //read http headers & load mimeheaders
                MimeHeaders mimeHeaders = new MimeHeaders();
                String key, value;
                // skip status line
                int i = 1;
                while (true) {
                    key = httpCon.getHeaderFieldKey(i);
                    value = httpCon.getHeaderField(i);
                    if (key == null && value == null) {
                        break;
                    }

                    if (key != null) {
                        StringTokenizer values = new StringTokenizer(value, ",");
                        while (values.hasMoreTokens()) {
                            mimeHeaders.addHeader(key, values.nextToken().trim());
                        }
                    }
                    i++;
                }
                InputStream httpInputStream;
                if (isFailure) {
                    httpInputStream = httpCon.getErrorStream();
                } else {
                    httpInputStream = httpCon.getInputStream();
                }

                soapMessage = new SOAPMessageImpl(httpInputStream, mimeHeaders);
                httpInputStream.close();
                httpCon.disconnect();

            } catch (SOAPException e) {
                throw e;
            } catch (Exception e) {
                throw new SOAPException(e.getMessage());
            }
        }
        return soapMessage;
    }

}
