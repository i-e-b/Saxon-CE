package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.instruct.ForEachGroup;
import client.net.sf.saxon.ce.lib.NamespaceConstant;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.pattern.PatternSponsor;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.value.EmptySequence;
import client.net.sf.saxon.ce.value.SequenceType;


/**
* Handler for xsl:for-each-group elements in stylesheet. This is a new instruction
* defined in XSLT 2.0
*/

public final class XSLForEachGroup extends StyleElement {

    private Expression select = null;
    private Expression groupBy = null;
    private Expression groupAdjacent = null;
    private Pattern starting = null;
    private Pattern ending = null;
    private Expression collationName;

    /**
    * Determine whether this node is an instruction.
    * @return true - it is an instruction
    */

    public boolean isInstruction() {
        return true;
    }

    /**
     * Specify that xsl:sort is a permitted child
     */

    protected boolean isPermittedChild(StyleElement child) {
        return (child instanceof XSLSort);
    }

    /**
    * Determine whether this type of element is allowed to contain a template-body
    * @return true: yes, it may contain a template-body
    */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {

        select = (Expression)checkAttribute("select", "e1");
        groupBy = (Expression)checkAttribute("group-by", "e");
        groupAdjacent = (Expression)checkAttribute("group-adjacent", "e");
        starting = (Pattern)checkAttribute("group-starting-with", "p");
        ending = (Pattern)checkAttribute("group-ending-with", "p");
        String collationAtt = (String)checkAttribute("collation", "w");
        checkForUnknownAttributes();

        int c = (groupBy==null ? 0 : 1) +
                (groupAdjacent==null ? 0 : 1) +
                (starting==null ? 0 : 1) +
                (ending==null ? 0 : 1);
        if (c!=1) {
            compileError("Exactly one of the attributes group-by, group-adjacent, group-starting-with, " +
                    "and group-ending-with must be specified", "XTSE1080");
        }

        if (collationAtt != null) {
            if (groupBy==null && groupAdjacent==null) {
                compileError("A collation may be specified only if group-by or group-adjacent is specified", "XTSE1090");
            } else {
                collationName = makeAttributeValueTemplate(collationAtt);
                if (collationName instanceof StringLiteral) {
                    String collation = ((StringLiteral)collationName).getStringValue();
                    URI collationURI;
                    try {
                        collationURI = new URI(collation, true);
                        if (!collationURI.isAbsolute()) {
                            URI base = new URI(getBaseURI());
                            collationURI = base.resolve(collationURI.toString());
                            collationName = new StringLiteral(collationURI.toString());
                        }
                    } catch (URI.URISyntaxException err) {
                        compileError("Collation name '" + collationName + "' is not a valid URI", "XTDE1110");
                        collationName = new StringLiteral(NamespaceConstant.CODEPOINT_COLLATION_URI);
                    }
                }
            }
        } else {
            String defaultCollation = getDefaultCollationName();
            if (defaultCollation != null) {
                collationName = new StringLiteral(defaultCollation);
            }
        }
    }

    public void validate(Declaration decl) throws XPathException {
        checkSortComesFirst(false);
        select = typeCheck(select);

        ExpressionVisitor visitor = makeExpressionVisitor();
        if (groupBy != null) {
            groupBy = typeCheck(groupBy);
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/group-by", 0);
                //role.setSourceLocator(locator);
                groupBy = TypeChecker.staticTypeCheck(groupBy,
                        SequenceType.ATOMIC_SEQUENCE,
                        false, role);
            } catch (XPathException err) {
                compileError(err);
            }
        } else if (groupAdjacent != null) {
            groupAdjacent = typeCheck(groupAdjacent);
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/group-adjacent", 0);
                //role.setSourceLocator(locator);
                role.setErrorCode("XTTE1100");
                groupAdjacent = TypeChecker.staticTypeCheck(groupAdjacent,
                        SequenceType.SINGLE_ATOMIC,
                        false, role);
            } catch (XPathException err) {
                compileError(err);
            }
        }

        starting = typeCheck("starting", starting);
        ending = typeCheck("ending", ending);

        if (starting != null || ending != null) {
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:for-each-group/select", 0);
                //role.setSourceLocator(locator);
                role.setErrorCode("XTTE1120");
                select = TypeChecker.staticTypeCheck(select,
                                            SequenceType.NODE_SEQUENCE,
                                            false, role);
            } catch (XPathException err) {
                String prefix = (starting != null ?
                        "With group-starting-with attribute: " :
                        "With group-ending-with attribute: ");
                compileError(prefix + err.getMessage(), err.getErrorCodeQName());
            }
        }
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {

        int algorithm = 0;
        Expression key = null;
        if (groupBy != null) {
            algorithm = ForEachGroup.GROUP_BY;
            key = groupBy;
        } else if (groupAdjacent != null) {
            algorithm = ForEachGroup.GROUP_ADJACENT;
            key = groupAdjacent;
        } else if (starting != null) {
            algorithm = ForEachGroup.GROUP_STARTING;
            key = new PatternSponsor(starting);
        } else if (ending != null) {
            algorithm = ForEachGroup.GROUP_ENDING;
            key = new PatternSponsor(ending);
        }

//        Block action = new Block();
//        compileChildren(exec, action, true);
        Expression action = compileSequenceConstructor(exec, decl);
        if (action == null) {
            // body of for-each is empty: it's a no-op.
            return new Literal(EmptySequence.getInstance());
        }
        try {
            return new ForEachGroup(    select,
                                        makeExpressionVisitor().simplify(action),
                                        algorithm,
                                        key,
                                        collationName,
                                        getBaseURI(),
                                        makeSortKeys(decl) );
        } catch (XPathException e) {
            compileError(e);
            return null;
        }

    }

}

// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
