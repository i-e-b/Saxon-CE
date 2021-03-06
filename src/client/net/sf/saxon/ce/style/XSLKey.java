package client.net.sf.saxon.ce.style;
import client.net.sf.saxon.ce.expr.*;
import client.net.sf.saxon.ce.expr.instruct.Executable;
import client.net.sf.saxon.ce.expr.sort.CodepointCollator;
import client.net.sf.saxon.ce.lib.StringCollator;
import client.net.sf.saxon.ce.om.StructuredQName;
import client.net.sf.saxon.ce.pattern.Pattern;
import client.net.sf.saxon.ce.trans.Err;
import client.net.sf.saxon.ce.trans.KeyDefinition;
import client.net.sf.saxon.ce.trans.KeyManager;
import client.net.sf.saxon.ce.trans.XPathException;
import client.net.sf.saxon.ce.tree.util.URI;
import client.net.sf.saxon.ce.type.AtomicType;
import client.net.sf.saxon.ce.value.SequenceType;


/**
* Handler for xsl:key elements in stylesheet. <br>
*/

public class XSLKey extends StyleElement implements StylesheetProcedure {

    private Pattern match;
    private Expression use;
    private String collationName;

    @Override
    public boolean isDeclaration() {
        return true;
    }

    /**
      * Determine whether this type of element is allowed to contain a sequence constructor
      * @return true: yes, it may contain a sequence constructor
      */

    public boolean mayContainSequenceConstructor() {
        return true;
    }

    public void prepareAttributes() throws XPathException {
        setObjectName((StructuredQName)checkAttribute("name", "q1"));
        use = (Expression)checkAttribute("use", "e");
        match = (Pattern)checkAttribute("match", "p1");
        collationName = (String)checkAttribute("collation", "w");
        checkForUnknownAttributes();
    }

    public StructuredQName getKeyName() {
    	return getObjectName();
    }

    public void validate(Declaration decl) throws XPathException {

        checkTopLevel(null);
        if (use!=null) {
            // the value can be supplied as a content constructor in place of a use expression
            if (hasChildNodes()) {
                compileError("An xsl:key element with a @use attribute must be empty", "XTSE1205");
            }
            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:key/use", 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                use = TypeChecker.staticTypeCheck(
                                use,
                                SequenceType.makeSequenceType(AtomicType.ANY_ATOMIC, StaticProperty.ALLOWS_ZERO_OR_MORE),
                                false, role);
            } catch (XPathException err) {
                compileError(err);
            }
        } else {
            if (!hasChildNodes()) {
                compileError("An xsl:key element must either have a @use attribute or have content", "XTSE1205");
            }
        }
        use = typeCheck(use);
        match = typeCheck("match", match);

        // Do a further check that the use expression makes sense in the context of the match pattern
        if (use != null) {
            use = makeExpressionVisitor().typeCheck(use, match.getNodeTest());
        }

        if (collationName != null) {
            URI collationURI;
            try {
                collationURI = new URI(collationName, true);
                if (!collationURI.isAbsolute()) {
                    URI base = new URI(getBaseURI());
                    collationURI = base.resolve(collationURI.toString());
                    collationName = collationURI.toString();
                }
            } catch (URI.URISyntaxException err) {
                compileError("Collation name '" + collationName + "' is not a valid URI");
                //collationName = NamespaceConstant.CODEPOINT_COLLATION_URI;
            }
        } else {
            collationName = getDefaultCollationName();
        }
    }

    protected void index(Declaration decl, PrincipalStylesheetModule top) throws XPathException {
        StructuredQName keyName = getKeyName();
        if (keyName != null) {
            top.getExecutable().getKeyManager().preRegisterKeyDefinition(keyName);
        }
    }

    public Expression compile(Executable exec, Declaration decl) throws XPathException {
        StaticContext env = getStaticContext();
        StringCollator collator = null;
        if (collationName != null) {
            collator = getConfiguration().getNamedCollation(collationName);
            if (collator==null) {
                compileError("The collation name " + Err.wrap(collationName, Err.URI) + " is not recognized", "XTSE1210");
                collator = CodepointCollator.getInstance();
            }
            if (collator instanceof CodepointCollator) {
                // if the user explicitly asks for the codepoint collation, treat it as if they hadn't asked
                collator = null;
                collationName = null;

            } else {
                compileError("The collation used for xsl:key must be capable of generating collation keys", "XTSE1210");
            }
        }

        if (use==null) {
            Expression body = compileSequenceConstructor(exec, decl);

            try {
                ExpressionVisitor visitor = makeExpressionVisitor();
                use = new Atomizer(body);
                use = visitor.simplify(use);
            } catch (XPathException e) {
                compileError(e);
            }

            try {
                RoleLocator role =
                    new RoleLocator(RoleLocator.INSTRUCTION, "xsl:key/use", 0);
                //role.setSourceLocator(new ExpressionLocation(this));
                use = TypeChecker.staticTypeCheck(
                                use,
                                SequenceType.makeSequenceType(AtomicType.ANY_ATOMIC, StaticProperty.ALLOWS_ZERO_OR_MORE),
                                false, role);
                // Do a further check that the use expression makes sense in the context of the match pattern
                use = makeExpressionVisitor().typeCheck(use, match.getNodeTest());


            } catch (XPathException err) {
                compileError(err);
            }
        }
        AtomicType useType = (AtomicType)use.getItemType();
        if (xPath10ModeIsEnabled()) {
            if (!useType.equals(AtomicType.STRING) && !useType.equals(AtomicType.UNTYPED_ATOMIC)) {
                use = new AtomicSequenceConverter(use, AtomicType.STRING);
                useType = AtomicType.STRING;
            }
        }
        int slots = match.allocateSlots(0);
        allocatePatternSlots(slots);


        KeyManager km = getExecutable().getKeyManager();
        KeyDefinition keydef = new KeyDefinition(match, use, collationName, collator);
        keydef.setIndexedItemType(useType);
        //keydef.setStackFrameMap(stackFrameMap);
        keydef.setSourceLocator(this);
        keydef.setExecutable(getExecutable());
        keydef.setBackwardsCompatible(xPath10ModeIsEnabled());
        keydef.allocateSlots(0);
        try {
            km.addKeyDefinition(getObjectName(), keydef, exec.getConfiguration());
        } catch (XPathException err) {
            compileError(err);
        }
        return null;
    }

    /**
     * Optimize the stylesheet construct
     * @param declaration
     */

    public void optimize(Declaration declaration) throws XPathException {
        // already done earlier
    }
}
// This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
// If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
// This Source Code Form is “Incompatible With Secondary Licenses”, as defined by the Mozilla Public License, v. 2.0.
