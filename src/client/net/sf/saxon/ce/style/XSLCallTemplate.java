package client.net.sf.saxon.ce.style;

import client.net.sf.saxon.ce.LogController;
import client.net.sf.saxon.ce.expr.Expression;
import client.net.sf.saxon.ce.expr.instruct.CallTemplate;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.Template;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.linked.NodeImpl;
import client.net.sf.saxon.ce.type.AnyItemType;
import client.net.sf.saxon.ce.type.ItemType;
import client.net.sf.saxon.ce.value.SequenceType;
import com.google.gwt.logging.client.LogConfiguration;

/**
 * An xsl:call-template element in the stylesheet
 */

public class XSLCallTemplate extends StyleElement {

    private StructuredQName calledTemplateName;   // the name of the called template
    private XSLTemplate template = null;
    private boolean useTailRecursion = false;

    /**
     * Determine whether this node is an instruction.
     *
     * @return true - it is an instruction
     */

    public boolean isInstruction() {
        return true;
    }

    private boolean gettingReturnedItemType = false;

    /**
     * Determine the type of item returned by this instruction (only relevant if
     * it is an instruction).
     *
     * @return the item type returned
     */

    protected ItemType getReturnedItemType() {
        if (template == null || gettingReturnedItemType) {
            return AnyItemType.getInstance();
        } else {
            // protect against infinite recursion
            gettingReturnedItemType = true;
            ItemType result = template.getReturnedItemType();
            gettingReturnedItemType = false;
            return result;
        }
    }

    public void prepareAttributes() throws XPathException {

        calledTemplateName = (StructuredQName) checkAttribute("name", "q1");
        checkForUnknownAttributes();

//		AttributeCollection atts = getAttributeList();
//
//        String nameAttribute = null;
//
//		for (int a=0; a<atts.getLength(); a++) {
//			StructuredQName qn = atts.getStructuredQName(a);
//            String f = qn.getClarkName();
//			if (f.equals("name")) {
//        		nameAttribute = Whitespace.trim(atts.getValue(a));
//        	} else {
//        		checkUnknownAttribute(qn);
//        	}
//        }
//
//        if (nameAttribute==null) {
//            calledTemplateName = new StructuredQName("saxon", NamespaceConstant.SAXON, "error-template");
//            reportAbsence("name");
//            return;
//        }
//
//        try {
//            calledTemplateName = makeQName(nameAttribute);
//        } catch (NamespaceException err) {
//            calledTemplateName = new StructuredQName("saxon", NamespaceConstant.SAXON, "error-template");
//            compileError(err.getMessage(), "XTSE0280");
//        } catch (XPathException err) {
//            calledTemplateName = new StructuredQName("saxon", NamespaceConstant.SAXON, "error-template");
//            compileError(err.getMessage(), err.getErrorCodeQName());
//        }
    }

    public void validate(Declaration decl) throws XPathException {
        onlyAllow("with-param");

        if (!(calledTemplateName.getNamespaceURI().equals(NamespaceConstant.SAXON) &&
                calledTemplateName.getLocalName().equals("error-template"))) {
            template = findTemplate(calledTemplateName);
        }
    }

    public void postValidate() throws XPathException {
        // check that a parameter is supplied for each required parameter
        // of the called template

        if (template != null) {
            for (NodeImpl param : template.allChildren()) {
                if (param instanceof XSLParam && ((XSLParam) param).isRequiredParam()
                        && !((XSLParam) param).isTunnelParam()) {
                    boolean ok = false;
                    for (NodeImpl child : allChildren()) {
                        if (child instanceof XSLWithParam &&
                                ((XSLWithParam) child).getVariableQName().equals(
                                        ((XSLParam) param).getVariableQName())) {
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        compileError("No value supplied for required parameter " +
                                Err.wrap(((XSLParam) param).getVariableDisplayName(), Err.VARIABLE), "XTSE0690");
                    }
                }
            }


            // check that every supplied parameter is declared in the called
            // template

            for (NodeImpl w : allChildren()) {
                if (w instanceof XSLWithParam && !((XSLWithParam) w).isTunnelParam()) {
                    XSLWithParam withParam = (XSLWithParam) w;
                    boolean ok = false;
                    for (NodeImpl param : template.allChildren()) {
                        if (param instanceof XSLParam &&
                                ((XSLParam) param).getVariableQName().equals(withParam.getVariableQName())
                            /* TODO spec bug 10934: && !((XSLParam)param).isTunnelParam() */
                                ) {
                            ok = true;
                            SequenceType required = ((XSLParam) param).getRequiredType();
                            withParam.checkAgainstRequiredType(required);
                            break;
                        }
                    }
                    if (!ok) {
                        if (!xPath10ModeIsEnabled()) {
                            compileError("Parameter " +
                                    withParam.getVariableDisplayName() +
                                    " is not declared in the called template", "XTSE0680");
                        }
                    }
                }
            }
        }
    }

    private XSLTemplate findTemplate(StructuredQName templateName)
            throws XPathException {

        PrincipalStylesheetModule psm = getPrincipalStylesheetModule();
        XSLTemplate template = psm.getNamedTemplate(templateName);
        if (template == null) {
            compileError("No template exists named " + calledTemplateName, "XTSE0650");
        }
        return template;
    }

    /**
     * Mark tail-recursive calls on templates and functions.
     * For most instructions, this does nothing.
     */

    public boolean markTailCalls() {
        useTailRecursion = true;
        return true;
    }


    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        Template target;

        if (template == null) {
            return null;   // error already reported
        }
        target = template.getCompiledTemplate();

        CallTemplate call = new CallTemplate(target, useTailRecursion);
        call.setActualParameters(getWithParamInstructions(exec, decl, false, call),
                getWithParamInstructions(exec, decl, true, call));
        if (LogConfiguration.loggingIsEnabled() && LogController.traceIsEnabled()) {
            call.AddTraceProperty("name", calledTemplateName.getDisplayName());
        }

        return call;
    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
