/*
 * Copyright (C) 2005 Alfresco, Inc.
 *
 * Licensed under the Alfresco Network License. You may obtain a
 * copy of the License at
 *
 *   http://www.alfrescosoftware.com/legal/
 *
 * Please view the license relevant to your network subscription.
 *
 * BY CLICKING THE "I UNDERSTAND AND ACCEPT" BOX, OR INSTALLING,  
 * READING OR USING ALFRESCO'S Network SOFTWARE (THE "SOFTWARE"),  
 * YOU ARE AGREEING ON BEHALF OF THE ENTITY LICENSING THE SOFTWARE    
 * ("COMPANY") THAT COMPANY WILL BE BOUND BY AND IS BECOMING A PARTY TO 
 * THIS ALFRESCO NETWORK AGREEMENT ("AGREEMENT") AND THAT YOU HAVE THE   
 * AUTHORITY TO BIND COMPANY. IF COMPANY DOES NOT AGREE TO ALL OF THE   
 * TERMS OF THIS AGREEMENT, DO NOT SELECT THE "I UNDERSTAND AND AGREE"   
 * BOX AND DO NOT INSTALL THE SOFTWARE OR VIEW THE SOURCE CODE. COMPANY   
 * HAS NOT BECOME A LICENSEE OF, AND IS NOT AUTHORIZED TO USE THE    
 * SOFTWARE UNLESS AND UNTIL IT HAS AGREED TO BE BOUND BY THESE LICENSE  
 * TERMS. THE "EFFECTIVE DATE" FOR THIS AGREEMENT SHALL BE THE DAY YOU  
 * CHECK THE "I UNDERSTAND AND ACCEPT" BOX.
 */
package org.alfresco.repo.security.authentication.ldap;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.importer.ExportSource;
import org.alfresco.repo.importer.ExportSourceImporterException;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.ApplicationContextHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class LDAPPersonExportSource implements ExportSource
{
    private static Log s_logger = LogFactory.getLog(LDAPPersonExportSource.class);
    
    private String personQuery = "(objectclass=inetOrgPerson)";

    private String searchBase;

    private String userIdAttributeName;

    private LDAPInitialDirContextFactory ldapInitialContextFactory;

    private PersonService personService;

    private Map<String, String> attributeMapping;

    private NamespaceService namespaceService;

    private String defaultHomeFolder;

    public LDAPPersonExportSource()
    {
        super();
    }

    public void setPersonQuery(String personQuery)
    {
        this.personQuery = personQuery;
    }

    public void setSearchBase(String searchBase)
    {
        this.searchBase = searchBase;
    }

    public void setUserIdAttributeName(String userIdAttributeName)
    {
        this.userIdAttributeName = userIdAttributeName;
    }

    public void setLDAPInitialDirContextFactory(LDAPInitialDirContextFactory ldapInitialDirContextFactory)
    {
        this.ldapInitialContextFactory = ldapInitialDirContextFactory;
    }

    public void setPersonService(PersonService personService)
    {
        this.personService = personService;
    }

    public void setDefaultHomeFolder(String defaultHomeFolder)
    {
        this.defaultHomeFolder = defaultHomeFolder;
    }

    public void setNamespaceService(NamespaceService namespaceService)
    {
        this.namespaceService = namespaceService;
    }

    public void setAttributeMapping(Map<String, String> attributeMapping)
    {
        this.attributeMapping = attributeMapping;
    }

    public void generateExport(XMLWriter writer)
    {
        QName nodeUUID = QName.createQName("sys:node-uuid", namespaceService);

        Collection<String> prefixes = namespaceService.getPrefixes();
        QName childQName = QName.createQName(NamespaceService.REPOSITORY_VIEW_PREFIX, "childName", namespaceService);

        try
        {
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute(NamespaceService.REPOSITORY_VIEW_1_0_URI, childQName.getLocalName(), childQName
                    .toPrefixString(), null, ContentModel.TYPE_PERSON.toPrefixString(namespaceService));

            writer.startDocument();

            for (String prefix : prefixes)
            {
                if (!prefix.equals("xml"))
                {
                    String uri = namespaceService.getNamespaceURI(prefix);
                    writer.startPrefixMapping(prefix, uri);
                }
            }

            writer.startElement(NamespaceService.REPOSITORY_VIEW_PREFIX, "view",
                    NamespaceService.REPOSITORY_VIEW_PREFIX + ":" + "view", new AttributesImpl());

            InitialDirContext ctx = null;
            try
            {
                ctx = ldapInitialContextFactory.getDefaultIntialDirContext();

                // Authentication has been successful.
                // Set the current user, they are now authenticated.

                SearchControls userSearchCtls = new SearchControls();
                userSearchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                System.out.println("COUNT "+userSearchCtls.getCountLimit());
                System.out.println("TIME "+userSearchCtls.getTimeLimit());
                userSearchCtls.setCountLimit(Integer.MAX_VALUE);
                
                NamingEnumeration searchResults = ctx.search(searchBase, personQuery, userSearchCtls);
                while (searchResults.hasMoreElements())
                {
                    SearchResult result = (SearchResult) searchResults.next();
                    Attributes attributes = result.getAttributes();
                    Attribute uidAttribute = attributes.get(userIdAttributeName);
                    String uid = (String) uidAttribute.get(0);

                    if(s_logger.isDebugEnabled())
                    {
                        s_logger.debug("Adding user for "+uid);
                    }
                    System.out.println("User "+uid);
                    
                    writer.startElement(ContentModel.TYPE_PERSON.getNamespaceURI(), ContentModel.TYPE_PERSON
                            .getLocalName(), ContentModel.TYPE_PERSON.toPrefixString(namespaceService), attrs);

                    // permissions

                    // owner

                    writer.startElement(ContentModel.ASPECT_OWNABLE.getNamespaceURI(), ContentModel.ASPECT_OWNABLE
                            .getLocalName(), ContentModel.ASPECT_OWNABLE.toPrefixString(namespaceService),
                            new AttributesImpl());

                    writer.endElement(ContentModel.ASPECT_OWNABLE.getNamespaceURI(), ContentModel.ASPECT_OWNABLE
                            .getLocalName(), ContentModel.ASPECT_OWNABLE.toPrefixString(namespaceService));

                    writer.startElement(ContentModel.PROP_OWNER.getNamespaceURI(), ContentModel.PROP_OWNER
                            .getLocalName(), ContentModel.PROP_OWNER.toPrefixString(namespaceService),
                            new AttributesImpl());

                    writer.characters(uid.toCharArray(), 0, uid.length());

                    writer.endElement(ContentModel.PROP_OWNER.getNamespaceURI(),
                            ContentModel.PROP_OWNER.getLocalName(), ContentModel.PROP_OWNER
                                    .toPrefixString(namespaceService));

                    for (String key : attributeMapping.keySet())
                    {
                        QName keyQName = QName.createQName(key, namespaceService);

                        writer.startElement(keyQName.getNamespaceURI(), keyQName.getLocalName(), keyQName
                                .toPrefixString(namespaceService), new AttributesImpl());

                        // cater for null
                        String attribute = attributeMapping.get(key);
                        if (attribute != null)
                        {
                            String value = (String) attributes.get(attribute).get(0);
                            if (value != null)
                            {
                                writer.characters(value.toCharArray(), 0, value.length());
                            }
                        }

                        writer.endElement(keyQName.getNamespaceURI(), keyQName.getLocalName(), keyQName
                                .toPrefixString(namespaceService));
                    }

                    // Default home folder

                    if (!(attributeMapping.keySet().contains(ContentModel.PROP_HOMEFOLDER.toString()) || attributeMapping
                            .keySet().contains(ContentModel.PROP_HOMEFOLDER.toPrefixString(namespaceService))))
                    {
                        // Only if we are creating the person for the first time
                        if (!personService.personExists(uid))
                        {
                            writer.startElement(ContentModel.PROP_HOMEFOLDER.getNamespaceURI(),
                                    ContentModel.PROP_HOMEFOLDER.getLocalName(), ContentModel.PROP_HOMEFOLDER
                                            .toPrefixString(namespaceService), new AttributesImpl());

                            if (defaultHomeFolder != null)
                            {
                                writer.characters(defaultHomeFolder.toCharArray(), 0, defaultHomeFolder.length());
                            }

                            writer.endElement(ContentModel.PROP_HOMEFOLDER.getNamespaceURI(),
                                    ContentModel.PROP_HOMEFOLDER.getLocalName(), ContentModel.PROP_HOMEFOLDER
                                            .toPrefixString(namespaceService));
                        }
                    }

                    if (personService.personExists(uid))
                    {
                        String uguid = personService.getPerson(uid).getId();

                        writer.startElement(nodeUUID.getNamespaceURI(), nodeUUID.getLocalName(), nodeUUID
                                .toPrefixString(namespaceService), new AttributesImpl());

                        writer.characters(uguid.toCharArray(), 0, uguid.length());

                        writer.endElement(nodeUUID.getNamespaceURI(), nodeUUID.getLocalName(), nodeUUID
                                .toPrefixString(namespaceService));
                    }
                    writer.endElement(ContentModel.TYPE_PERSON.getNamespaceURI(), ContentModel.TYPE_PERSON
                            .getLocalName(), ContentModel.TYPE_PERSON.toPrefixString(namespaceService));

                }

            }
            catch (NamingException e)
            {
                throw new ExportSourceImporterException("Failed to import people.", e);
            }
            finally
            {
                if (ctx != null)
                {
                    try
                    {
                        ctx.close();
                    }
                    catch (NamingException e)
                    {
                        throw new ExportSourceImporterException("Failed to import people.", e);
                    }
                }
            }

            for (String prefix : prefixes)
            {
                if (!prefix.equals("xml"))
                {
                    writer.endPrefixMapping(prefix);
                }
            }

            writer.endElement(NamespaceService.REPOSITORY_VIEW_PREFIX, "view", NamespaceService.REPOSITORY_VIEW_PREFIX
                    + ":" + "view");

            writer.endDocument();
        }
        catch (SAXException e)
        {
            throw new ExportSourceImporterException("Failed to create file for import.", e);
        }
    }

    public static void main(String[] args) throws IOException
    {
        ApplicationContext ctx = ApplicationContextHelper.getApplicationContext();
        ExportSource source = (ExportSource) ctx.getBean("ldapPeopleExportSource");

        File file = new File(args[0]);
        Writer writer = new BufferedWriter(new FileWriter(file));
        XMLWriter xmlWriter = createXMLExporter(writer);
        source.generateExport(xmlWriter);
        xmlWriter.close();

    }

    private static XMLWriter createXMLExporter(Writer writer)
    {
        // Define output format
        OutputFormat format = OutputFormat.createPrettyPrint();
        format.setNewLineAfterDeclaration(false);
        format.setIndentSize(3);
        format.setEncoding("UTF-8");

        // Construct an XML Exporter

        XMLWriter xmlWriter = new XMLWriter(writer, format);
        return xmlWriter;
    }
}