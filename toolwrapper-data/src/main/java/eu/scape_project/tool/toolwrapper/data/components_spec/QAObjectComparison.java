//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.11.27 at 03:47:31 PM WET 
//


package eu.scape_project.tool.toolwrapper.data.components_spec;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for QAObjectComparison complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="QAObjectComparison">
 *   &lt;complexContent>
 *     &lt;extension base="{http://scape-project.eu/component}Component">
 *       &lt;sequence>
 *         &lt;element name="acceptedMimetype" type="{http://scape-project.eu/component}AcceptedMimetype" maxOccurs="unbounded"/>
 *         &lt;element name="inputMeasure" type="{http://scape-project.eu/component}Measure" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="outputMeasure" type="{http://scape-project.eu/component}Measure" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="profile" use="required" type="{http://scape-project.eu/component}QAObjectComparisonProfile" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "QAObjectComparison", propOrder = {
    "acceptedMimetype",
    "inputMeasure",
    "outputMeasure"
})
public class QAObjectComparison
    extends Component
{

    @XmlElement(required = true)
    protected List<AcceptedMimetype> acceptedMimetype;
    protected List<Measure> inputMeasure;
    @XmlElement(required = true)
    protected List<Measure> outputMeasure;
    @XmlAttribute(required = true)
    protected String profile;

    /**
     * Gets the value of the acceptedMimetype property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the acceptedMimetype property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAcceptedMimetype().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AcceptedMimetype }
     * 
     * 
     */
    public List<AcceptedMimetype> getAcceptedMimetype() {
        if (acceptedMimetype == null) {
            acceptedMimetype = new ArrayList<AcceptedMimetype>();
        }
        return this.acceptedMimetype;
    }

    /**
     * Gets the value of the inputMeasure property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the inputMeasure property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getInputMeasure().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Measure }
     * 
     * 
     */
    public List<Measure> getInputMeasure() {
        if (inputMeasure == null) {
            inputMeasure = new ArrayList<Measure>();
        }
        return this.inputMeasure;
    }

    /**
     * Gets the value of the outputMeasure property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the outputMeasure property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOutputMeasure().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Measure }
     * 
     * 
     */
    public List<Measure> getOutputMeasure() {
        if (outputMeasure == null) {
            outputMeasure = new ArrayList<Measure>();
        }
        return this.outputMeasure;
    }

    /**
     * Gets the value of the profile property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProfile() {
        return profile;
    }

    /**
     * Sets the value of the profile property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProfile(String value) {
        this.profile = value;
    }

}
