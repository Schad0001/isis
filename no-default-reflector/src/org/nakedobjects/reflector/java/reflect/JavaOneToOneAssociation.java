package org.nakedobjects.reflector.java.reflect;

import org.nakedobjects.application.ValueParseException;
import org.nakedobjects.application.valueholder.BusinessValueHolder;
import org.nakedobjects.object.InvalidEntryException;
import org.nakedobjects.object.Naked;
import org.nakedobjects.object.NakedObject;
import org.nakedobjects.object.NakedObjects;
import org.nakedobjects.object.NakedValue;
import org.nakedobjects.object.TextEntryParseException;
import org.nakedobjects.object.control.Consent;
import org.nakedobjects.object.control.DefaultHint;
import org.nakedobjects.object.control.Hint;
import org.nakedobjects.object.reflect.MemberIdentifier;
import org.nakedobjects.object.reflect.OneToOnePeer;
import org.nakedobjects.object.reflect.ReflectionException;
import org.nakedobjects.reflector.java.control.SimpleFieldAbout;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Category;


public class JavaOneToOneAssociation extends JavaField implements OneToOnePeer {
    private final static Category LOG = Category.getInstance(JavaOneToOneAssociation.class);
    protected Method addMethod;
    protected Method removeMethod;
    protected Method setMethod;
    private boolean isObject;

    public JavaOneToOneAssociation(boolean isObject, MemberIdentifier identifier, Class type, Method get, Method set, Method add, Method remove, Method about, boolean derived) {
        super(identifier, type, get, about, derived);
        this.setMethod = set;
        this.addMethod = add;
        removeMethod = remove;
        this.isObject = isObject;
    }

    public void clearAssociation(NakedObject inObject, NakedObject associate) {
        LOG.debug("local clear association " + inObject + "/" + associate);

        try {
            if (removeMethod != null) {
                removeMethod.invoke(inObject.getObject(), new Object[] { associate.getObject() });
            } else {
                setMethod.invoke(inObject.getObject(), new Object[] { null });
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("set method expects a " + getType().getFullName() + " object; not a "
                    + associate.getClass().getName());
        } catch (InvocationTargetException e) {
            invocationException("Exception executing " + setMethod, e);
        } catch (IllegalAccessException ignore) {
            LOG.error("illegal access of " + setMethod, ignore);
            throw new RuntimeException(ignore.getMessage());
        }
    }

    private Naked get(NakedObject fromObject) {
        try {
            Object obj = getMethod.invoke(fromObject.getObject(), new Object[0]);

            if (obj == null) {
                return null;
            } else {
                Naked adapter = NakedObjects.getObjectLoader().createAdapterForValue(obj);
                if(adapter == null) {
                    adapter = NakedObjects.getObjectLoader().getAdapterForElseCreateAdapterForTransient(obj);
                }
                return adapter;
            }

        } catch (InvocationTargetException e) {
            invocationException("Exception executing " + getMethod, e);
            return null;
        } catch (IllegalAccessException ignore) {
            LOG.error("illegal access of " + getMethod, ignore);
            throw new ReflectionException(ignore);
        }
    }

    public Naked getAssociation(NakedObject fromObject) {
        return get(fromObject);
    }

    public Object getExtension(Class cls) {
        return null;
    }
    
    public Class[] getExtensions() {
        return new Class[0];
    }

    public String getName() {
        return null;
    }
    
    private Hint getHint(NakedObject object, Naked associate) {
        Method aboutMethod = getAboutMethod();

        Class parameter = getMethod.getReturnType();
        Object object2 = object.getObject();
        if (associate != null && associate.getObject() != null && !parameter.isAssignableFrom(associate.getObject().getClass())) {
            SimpleFieldAbout about = new SimpleFieldAbout(NakedObjects.getCurrentSession(), object2);
            about.unmodifiable("Invalid type: field must be set with a "
                    + NakedObjects.getSpecificationLoader().loadSpecification(parameter.getName()));
            return about;
        }

        if (aboutMethod == null) {
            DefaultHint hint = new DefaultHint();
/*            if(hint.getDescription().equals("") && associate != null) {
                hint.setDescription("Set field " + getName() + " to " + associate.getObject());
            }
   */         return hint;
        }

        try {
            SimpleFieldAbout hint = new SimpleFieldAbout(NakedObjects.getCurrentSession(), object2);
            Object[] parameters;
            if (aboutMethod.getParameterTypes().length == 2) {
                parameters = new Object[] { hint, associate == null ? null : associate.getObject() };
            } else {
                // default about
                parameters = new Object[] { hint };
            }
            aboutMethod.invoke(object2, parameters);
            if(hint.getDescription().equals("") && associate != null) {
                hint.setDescription("Set field " + getIdentifier() + " to " + associate.getObject());
            }
            return hint;
        } catch (InvocationTargetException e) {
            invocationException("Exception executing " + aboutMethod, e);
        } catch (IllegalAccessException ignore) {
            LOG.error("illegal access of " + aboutMethod, ignore);
        }

        return new DefaultHint();
    }

    public boolean hasAddMethod() {
        return addMethod != null;
    }

    /**
     * Set the data in an NakedObject. Passes in an existing object to for the
     * EO to reference.
     */
    public void initAssociation(NakedObject inObject, NakedObject associate) {
        LOG.debug("local set association " + getIdentifier() + " in " + inObject + " with " + associate);

        try {
            setMethod.invoke(inObject.getObject(), new Object[] { associate == null ? null : associate.getObject() });
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(setMethod + " method doesn't expect a " + associate.getObject().getClass().getName());
        } catch (InvocationTargetException e) {
            invocationException("Exception executing " + setMethod, e);
        } catch (IllegalAccessException ignore) {
            LOG.error("illegal access of " + setMethod, ignore);
            throw new RuntimeException(ignore.getMessage());
        }
    }

    /**
     * Set the data in an NakedObject. Passes in an existing object to for the
     * EO to reference.
     */
    public void initValue(NakedObject inObject, Object setValue) {
        LOG.debug("local initValue() " + getIdentifier() + " " + inObject.getOid() + "/" + setValue);

        try {
            if (setMethod == null) {
                BusinessValueHolder value = (BusinessValueHolder) getMethod.invoke(inObject.getObject(), new Object[] {});
                if (setValue instanceof String) {
                    value.parseUserEntry((String) setValue);
                } else if (setValue instanceof BusinessValueHolder) {
                    value.copyObject((BusinessValueHolder) setValue);
                }

            } else {
                setMethod.invoke(inObject.getObject(), new Object[] { setValue == null ? null : setValue });
            }
        } catch (InvocationTargetException e) {
            invocationException("Exception executing " + setMethod, e);
        } catch (IllegalAccessException ignore) {
            LOG.error("illegal access of " + setMethod, ignore);
        } catch (ValueParseException e) {
            LOG.error("parse error: " + setValue, e);
        }
    }

    public boolean isEmpty(NakedObject fromObject) {
        try {
            Object obj = getMethod.invoke(fromObject.getObject(), new Object[0]);
            if (obj instanceof BusinessValueHolder) {
                BusinessValueHolder value = (BusinessValueHolder) obj;
                return value.isEmpty();
            } else {
                return obj == null;
            }
        } catch (InvocationTargetException e) {
            invocationException("Exception executing " + getMethod, e);
            throw new ReflectionException(e);
        } catch (IllegalAccessException ignore) {
            LOG.error("illegal access of " + getMethod, ignore);
            throw new ReflectionException(ignore);
        }
    }
    
    public boolean isMandatory() {
        return false;
    }
    
    public boolean isObject() {
        return isObject;
    }

    public void parseTextEntry(NakedObject inObject, String text) throws TextEntryParseException, InvalidEntryException {
        if (setMethod == null) {
            try {
                Object obj = getMethod.invoke(inObject.getObject(), new Object[0]);
                BusinessValueHolder value = (BusinessValueHolder) obj;
                value.parseUserEntry(text);
                NakedObjects.getObjectPersistor().objectChanged(inObject);
            } catch (InvocationTargetException e) {
                invocationException("Exception executing " + getMethod, e);
            } catch (IllegalAccessException ignore) {
                LOG.error("illegal access of " + getMethod, ignore);
                throw new ReflectionException("Illegal access of " + getMethod, ignore);
            } catch (ValueParseException e) {
                throw new TextEntryParseException(e);
            }

        } else {
            try {
                setMethod.invoke(inObject.getObject(), new Object[] { text });
            } catch (InvocationTargetException e) {
                invocationException("Exception executing " + setMethod, e);
            } catch (IllegalAccessException ignore) {
                LOG.error("llegal access of " + setMethod, ignore);
                throw new ReflectionException("Illegal access of " + setMethod, ignore);
            }

        }
    }

    public void setAssociation(NakedObject inObject, NakedObject associate) {
        LOG.debug("local set association " + getIdentifier() + " in " + inObject + " with " + associate);

        try {
            if (associate == null) {
                if (removeMethod != null) {
                    removeMethod.invoke(inObject.getObject(), new Object[] { get(inObject) });
                } else {
                    setMethod.invoke(inObject.getObject(), new Object[] { associate.getObject() });
                }
            } else {
                if (hasAddMethod()) {
                    addMethod.invoke(inObject.getObject(), new Object[] { associate.getObject() });
                } else {
                    setMethod.invoke(inObject.getObject(), new Object[] { associate.getObject() });
                }
            }

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(setMethod + " method doesn't expect a " + associate.getClass().getName());
        } catch (InvocationTargetException e) {
            invocationException("Exception executing " + setMethod, e);
        } catch (IllegalAccessException ignore) {
            LOG.error("illegal access of " + setMethod, ignore);
            throw new RuntimeException(ignore.getMessage());
        }
    }

    public void setValue(NakedObject inObject, Object setValue) {
        LOG.debug("local setValue() " + inObject.getOid() + "/" + getIdentifier() + "/" + setValue);

        try {
            if (setMethod == null) {
                NakedObjects.getObjectPersistor().objectChanged(inObject);
            } else {
                setMethod.invoke(inObject.getObject(), new Object[] { setValue });
            }
        } catch (InvocationTargetException e) {
            invocationException("Exception executing " + setMethod, e);
        } catch (IllegalAccessException ignore) {
            LOG.error("illegal access of " + setMethod, ignore);
        } catch (ValueParseException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public String toString() {
        String methods = (getMethod == null ? "" : "GET") + (setMethod == null ? "" : " SET") + (addMethod == null ? "" : " ADD")
                + (removeMethod == null ? "" : " REMOVE");

        return "Association [name=\"" + getIdentifier() + "\", method=" + getMethod + ",about=" + getAboutMethod() + ", methods="
                + methods + ", type=" + getType() + " ]";
    }
    
    
    

    public Consent validAssociation(NakedObject inObject, NakedObject value) {
        return getHint(inObject, value).canUse();
     }
    
    public Consent validValue(NakedObject inObject, NakedValue value) {
        return getHint(inObject, value).canUse();
    }
    

    public Consent isEditable(NakedObject target) {
        return getHint(target, null).canUse();
    }

    public Consent isVisible(NakedObject target) {
        return getHint(target, null).canAccess();
    }

    public boolean isAccessible() {
        return true;
    }
    
    public String getDescription() {
        return "";
    }
}

/*
 * Naked Objects - a framework that exposes behaviourally complete business
 * objects directly to the user. Copyright (C) 2000 - 2005 Naked Objects Group
 * Ltd
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 * The authors can be contacted via www.nakedobjects.org (the registered address
 * of Naked Objects Group is Kingsway House, 123 Goldworth Road, Woking GU21
 * 1NR, UK).
 */
