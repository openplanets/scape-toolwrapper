//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.11.27 at 03:47:31 PM WET 
//


package eu.scape_project.tool.toolwrapper.data.components_spec;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MigrationPath complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MigrationPath">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fromMimetype" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="toMimetype" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MigrationPath", propOrder = {
    "fromMimetype",
    "toMimetype"
})
public class MigrationPath {

    @XmlElement(required = true)
    protected String fromMimetype;
    @XmlElement(required = true)
    protected String toMimetype;

    /**
     * Gets the value of the fromMimetype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFromMimetype() {
        return fromMimetype;
    }

    /**
     * Sets the value of the fromMimetype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFromMimetype(String value) {
        this.fromMimetype = value;
    }

    /**
     * Gets the value of the toMimetype property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getToMimetype() {
        return toMimetype;
    }

    /**
     * Sets the value of the toMimetype property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setToMimetype(String value) {
        this.toMimetype = value;
    }

}
