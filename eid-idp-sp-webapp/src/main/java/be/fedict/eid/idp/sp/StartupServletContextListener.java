package be.fedict.eid.idp.sp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.*;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;


public class StartupServletContextListener implements ServletContextListener {

    private static final Log LOG = LogFactory.getLog(StartupServletContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        try {
            bindComponent("be/fedict/eid/idp/sp/AuthenticationResponseServiceBean",
                    new AuthenticationResponseServiceBean());
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

    public static void bindComponent(String jndiName, Object component)
            throws NamingException {

        LOG.debug("bind component: " + jndiName);
        InitialContext initialContext = new InitialContext();
        String[] names = jndiName.split("/");
        Context context = initialContext;
        for (int idx = 0; idx < names.length - 1; idx++) {
            String name = names[idx];
            LOG.debug("name: " + name);
            NamingEnumeration<NameClassPair> listContent = context.list("");
            boolean subContextPresent = false;
            while (listContent.hasMore()) {
                NameClassPair nameClassPair = listContent.next();
                if (!name.equals(nameClassPair.getName())) {
                    continue;
                }
                subContextPresent = true;
            }
            if (!subContextPresent) {
                context = context.createSubcontext(name);
            } else {
                context = (Context) context.lookup(name);
            }
        }
        String name = names[names.length - 1];
        context.rebind(name, component);
    }
}
