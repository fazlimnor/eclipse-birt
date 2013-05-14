package org.eclipse.birt.report.engine.emitter.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.birt.core.framework.IBundle;
import org.eclipse.birt.core.framework.Platform;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public abstract class DefaultEmitterDescriptor
{

	protected Map initParams = null;
	protected Locale locale;
	protected Map<String, RenderOptionDefn> renderOptionDefns = new HashMap<String, RenderOptionDefn>( );
	protected IConfigurableOption[] options;

	private static final String OPTIONS_CONFIG_FILE = "RenderDefaults.cfg";

	private static final String RENDER_OPTIONS_FILE = "RenderOptions.xml";

	private static final String OPTION_QNAME = "option";

	private static final String OPTION_NAME = "name";

	private static final String OPTION_DEFAULT = "default";

	private static final String OPTION_ENABLED = "enabled";

	public void setInitParameters( Map params )
	{
		this.initParams = params;
	}

	public void setLocale( Locale locale )
	{
		if ( this.locale != locale )
		{
			this.locale = locale;
			initOptions( );
		}
	}

	protected abstract void initOptions( );
	
	public IConfigurableOptionObserver createOptionObserver( )
	{
		return null;
	}

	public String getDescription( )
	{
		return null;
	}

	public String getDisplayName( )
	{
		return null;
	}

	public String getID( )
	{
		return null;
	}

	protected void applyDefaultValues( )
	{
		// parse the default value from the config file first.
		for ( IConfigurableOption option : options )
		{
			applyDefaultValue( option );
		}
	}

	protected boolean loadDefaultValues( String bundleName )
	{
		try
		{
			loadCFGFile( bundleName );
			loadXMLFile( bundleName );
		}
		catch ( Exception e )
		{
			renderOptionDefns = null;
		}
		return renderOptionDefns != null && !renderOptionDefns.isEmpty( );
	}


	private void loadXMLFile( String bundleName ) throws IOException, Exception
	{
		URL url = getResourceURL( bundleName, RENDER_OPTIONS_FILE );
		if ( url != null )
		{
			InputStream in = url.openStream( );
			doLoadDefaultValues( in );
			in.close( );
		}
	}

	private void loadCFGFile( String bundleName ) throws IOException
	{
		URL url = getResourceURL( bundleName, OPTIONS_CONFIG_FILE );
		if ( url != null )
		{
			InputStream in = url.openStream( );
			Properties defaultValues = new Properties( );
			defaultValues.load( in );
			for ( Entry<Object, Object> entry : defaultValues.entrySet( ) )
			{
				String name = entry.getKey( ).toString( );
				String value = entry.getValue( ).toString( );
				renderOptionDefns.put( name, new RenderOptionDefn( name,
						value,
						true ) );
			}
			in.close( );
		}
	}

	protected void doLoadDefaultValues( InputStream in ) throws Exception
	{

		SAXParser parser = SAXParserFactory.newInstance( ).newSAXParser( );
		try
		{
			parser.parse( in, new RenderOptionHandler( ) );
		}
		finally
		{
			// even there is XML exception, need to release the resource.
			try
			{
				parser.reset( );
				parser = null;
			}
			catch ( Exception e1 )
			{

			}
		}
	}

	private void applyDefaultValue( IConfigurableOption option )
	{
		if ( renderOptionDefns == null || renderOptionDefns.isEmpty( ) )
		{
			return;
		}
		RenderOptionDefn defn = renderOptionDefns.get( option.getName( ) );
		if ( defn != null )
		{
			String value = defn.getValue( );
			ConfigurableOption optionImpl = (ConfigurableOption) option;
			optionImpl.setEnabled( defn.isEnabled( ) );
			switch ( option.getDataType( ) )
			{
				case STRING :
					optionImpl.setDefaultValue( value );
					break;
				case BOOLEAN :
					optionImpl.setDefaultValue( Boolean.valueOf( value ) );
					break;
				case INTEGER :
					Integer intValue = null;
					try
					{
						intValue = Integer.decode( value );
					}
					catch ( NumberFormatException e )
					{
						break;
					}
					optionImpl.setDefaultValue( intValue );
					break;
				case FLOAT :
					Float floatValue = null;
					try
					{
						floatValue = Float.valueOf( value );
					}
					catch ( NumberFormatException e )
					{
						break;
					}
					optionImpl.setDefaultValue( floatValue );
					break;
				default :
					break;
			}
		}
	}

	protected URL getResourceURL( String bundleName, String resourceName )
	{
		IBundle bundle = Platform.getBundle( bundleName ); //$NON-NLS-1$
		if ( bundle != null )
		{
			return bundle.getEntry( resourceName );
		}
		return null;
	}

	protected class RenderOptionHandler extends DefaultHandler
	{

		@Override
		public void startElement( String uri, String localName, String qName,
				Attributes attributes ) throws SAXException
		{
			if ( OPTION_QNAME.equalsIgnoreCase( qName ) )
			{
				String name = attributes.getValue( OPTION_NAME );
				if ( !isEmpty( name ) )
				{
					String defualt = attributes.getValue( OPTION_DEFAULT );
					Boolean enabled = Boolean.TRUE;
					String enabledStr = attributes.getValue( OPTION_ENABLED );
					if ( !isEmpty( enabledStr ) )
					{
						enabled = Boolean.valueOf( enabledStr );
					}
					renderOptionDefns.put( name, new RenderOptionDefn( name,
							defualt,
							enabled ) );
				}
			}
		}

	}

	private boolean isEmpty( String str )
	{
		return str == null || str.length( ) == 0;
	}
}
