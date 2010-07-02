// Metawidget
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package org.metawidget.util;

import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.logging.LogFactory;

/**
 * Utilities for working with Logging.
 * <p>
 * Note: we're not trying to create <em>another</em> logging framework here! We're just trying to
 * use Commons Logging where available, and java.util Logging where it's not. Most web containers
 * will prefer Commons Logging, but we don't want to have to ship commons-logging.jar with, say, a
 * Swing applet.
 *
 * @author Richard Kennard
 */

public final class LogUtils {

	//
	// Public statics
	//

	/**
	 * Where possible, returns an implementation of Commons Logging. For those applications that do
	 * not use Commons Logging, returns an implementation of java.util Logging.
	 * <p>
	 * In general, Commons Logging is the better choice. However, that introduces a mandatory JAR
	 * dependency which we want to avoid.
	 * <p>
	 * Note: we're not trying to create <em>another</em> logging framework here! We're just trying
	 * to use Commons Logging where available, and java.util Logging where it's not. Most web
	 * containers will prefer Commons Logging, but we don't want to have to ship commons-logging.jar
	 * with, say, a Swing applet.
	 */

	public static Log getLog( Class<?> clazz ) {

		try {
			return new CommonsLog( clazz );
		} catch ( NoClassDefFoundError e ) {
			return new UtilLog( clazz.getName() );
		}
	}

	/**
	 * Common logging interface.
	 * <p>
	 * Note: we're not trying to create <em>another</em> logging framework here! We're just trying
	 * to use Commons Logging where available, and java.util Logging where it's not. Most web
	 * containers will prefer Commons Logging, but we don't want to have to ship commons-logging.jar
	 * with, say, a Swing applet.
	 */

	public interface Log {

		//
		// Methods
		//

		boolean isTraceEnabled();

		/**
		 * Log a trace message.
		 *
		 * @param debug
		 *            message to log
		 * @param arguments
		 *            array of arguments that will be merged into the message using standard
		 *            <code>java.text.MessageFormat</code> notation. As a special case, if the last
		 *            argument is a Throwable but is not referenced in the message, its stack trace
		 *            will be printed
		 */

		void trace( String trace, Object... arguments );

		void trace( String trace, Throwable throwable );

		boolean isDebugEnabled();

		/**
		 * Log a debug message.
		 *
		 * @param debug
		 *            message to log
		 * @param arguments
		 *            array of arguments that will be merged into the message using standard
		 *            <code>java.text.MessageFormat</code> notation. As a special case, if the last
		 *            argument is a Throwable but is not referenced in the message, its stack trace
		 *            will be printed
		 */

		void debug( String debug, Object... arguments );

		void debug( String debug, Throwable throwable );

		boolean isInfoEnabled();

		/**
		 * Log an info message.
		 *
		 * @param debug
		 *            message to log
		 * @param arguments
		 *            array of arguments that will be merged into the message using standard
		 *            <code>java.text.MessageFormat</code> notation. As a special case, if the last
		 *            argument is a Throwable but is not referenced in the message, its stack trace
		 *            will be printed
		 */

		void info( String info, Object... arguments );

		void info( String info, Throwable throwable );

		boolean isWarnEnabled();

		/**
		 * Log a warning message.
		 *
		 * @param debug
		 *            message to log
		 * @param arguments
		 *            array of arguments that will be merged into the message using standard
		 *            <code>java.text.MessageFormat</code> notation. As a special case, if the last
		 *            argument is a Throwable but is not referenced in the message, its stack trace
		 *            will be printed
		 */

		void warn( String warning, Object... arguments );

		void warn( String warning, Throwable throwable );

		boolean isErrorEnabled();

		/**
		 * Log an error message.
		 *
		 * @param debug
		 *            message to log
		 * @param arguments
		 *            array of arguments that will be merged into the message using standard
		 *            <code>java.text.MessageFormat</code> notation. As a special case, if the last
		 *            argument is a Throwable but is not referenced in the message, its stack trace
		 *            will be printed
		 */

		void error( String error, Object... arguments );

		void error( String error, Throwable throwable );
	}

	//
	// Private statics
	//

	/**
	 * Lightweight field that stores the last message sent to <code>trace</code>. Intended for unit
	 * tests.
	 */

	/* package private */static String	LAST_TRACE_MESSAGE;

	/**
	 * Lightweight field that stores the last message sent to <code>Log.debug</code>. Intended for
	 * unit tests.
	 */

	/* package private */static String	LAST_DEBUG_MESSAGE;

	/**
	 * Lightweight field that stores the last message sent to <code>Log.info</code>. Intended for
	 * unit tests.
	 */

	/* package private */static String	LAST_INFO_MESSAGE;

	/**
	 * Lightweight field that stores the last message sent to <code>Log.warn</code>. Intended for
	 * unit tests.
	 */

	/* package private */static String	LAST_WARN_MESSAGE;

	/**
	 * Logging implementation that uses <code>java.util.Logger</code>.
	 */

	private static class UtilLog
		implements Log {

		//
		// Private members
		//

		private Logger	mLogger;

		//
		// Constructor
		//

		public UtilLog( String logger ) {

			mLogger = Logger.getLogger( logger );
		}

		//
		// Public methods
		//

		public boolean isTraceEnabled() {

			return mLogger.isLoggable( Level.FINER );
		}

		public void trace( String trace, Object... arguments ) {

			LAST_TRACE_MESSAGE = log( Level.FINER, trace, arguments );
		}

		public void trace( String trace, Throwable throwable ) {

			mLogger.log( Level.FINER, trace, throwable );
		}

		public boolean isDebugEnabled() {

			return mLogger.isLoggable( Level.FINE );
		}

		public void debug( String debug, Object... arguments ) {

			LAST_DEBUG_MESSAGE = log( Level.FINE, debug, arguments );
		}

		public void debug( String debug, Throwable throwable ) {

			mLogger.log( Level.FINE, debug, throwable );
		}

		public boolean isInfoEnabled() {

			return mLogger.isLoggable( Level.INFO );
		}

		public void info( String info, Object... arguments ) {

			LAST_INFO_MESSAGE = log( Level.INFO, info, arguments );
		}

		public void info( String info, Throwable throwable ) {

			mLogger.log( Level.INFO, info, throwable );
		}

		public boolean isWarnEnabled() {

			return mLogger.isLoggable( Level.WARNING );
		}

		public void warn( String warning, Object... arguments ) {

			LAST_WARN_MESSAGE = log( Level.WARNING, warning, arguments );
		}

		public void warn( String warning, Throwable throwable ) {

			LAST_WARN_MESSAGE = warning;

			mLogger.log( Level.WARNING, warning, throwable );
		}

		public boolean isErrorEnabled() {

			return mLogger.isLoggable( Level.SEVERE );
		}

		public void error( String error, Object... arguments ) {

			log( Level.SEVERE, error, arguments );
		}

		public void error( String error, Throwable throwable ) {

			mLogger.log( Level.SEVERE, error, throwable );
		}

		//
		// Private methods
		//

		private String log( Level level, String message, Object... arguments ) {

			// Support fast cases with no arguments

			int lastArgument = arguments.length - 1;

			if ( lastArgument == -1 ) {
				mLogger.log( level, message );
				return message;
			}

			// Format the message

			String logged = MessageFormat.format( message, arguments );

			// Support cases with an unused Throwable on the end

			if ( arguments[lastArgument] instanceof Throwable && message.indexOf( "{" + lastArgument + "}" ) == -1 ) {
				mLogger.log( level, logged, (Throwable) arguments[lastArgument] );
				lastArgument--;
			} else {
				mLogger.log( level, logged );
			}

			if ( message.indexOf( "{" + lastArgument + "}" ) == -1 ) {
				throw new RuntimeException( "Given " + ( lastArgument + 1 ) + " arguments to log, but no {" + lastArgument + "} in message '" + message + "'" );
			}

			return logged;
		}
	}

	/**
	 * Logging implementation that uses <code>org.apache.commons.logging.Log</code>.
	 */

	private static class CommonsLog
		implements Log {

		//
		// Private members
		//

		private org.apache.commons.logging.Log	mLog;

		//
		// Constructor
		//

		public CommonsLog( Class<?> clazz ) {

			mLog = LogFactory.getLog( clazz );
		}

		//
		// Methods
		//

		public boolean isTraceEnabled() {

			return mLog.isTraceEnabled();
		}

		public void trace( String trace, Object... arguments ) {

			int lastArgument = arguments.length - 1;

			if ( lastArgument == -1 ) {
				LAST_TRACE_MESSAGE = trace;
				mLog.trace( trace );
			} else {
				String logged = MessageFormat.format( trace, arguments );
				LAST_TRACE_MESSAGE = logged;
				if ( arguments[lastArgument] instanceof Throwable && trace.indexOf( "{" + lastArgument + "}" ) == -1 ) {
					mLog.trace( logged, (Throwable) arguments[lastArgument] );
					lastArgument--;
				} else {
					mLog.trace( logged );
				}

				if ( trace.indexOf( "{" + lastArgument + "}" ) == -1 ) {
					throw new RuntimeException( "Given " + ( lastArgument + 1 ) + " arguments to log, but no {" + lastArgument + "} in message '" + trace + "'" );
				}
			}
		}

		public void trace( String trace, Throwable throwable ) {

			mLog.trace( trace, throwable );
		}

		public boolean isDebugEnabled() {

			return mLog.isDebugEnabled();
		}

		public void debug( String debug, Object... arguments ) {

			int lastArgument = arguments.length - 1;

			if ( lastArgument == -1 ) {
				LAST_DEBUG_MESSAGE = debug;
				mLog.debug( debug );
			} else {
				String logged = MessageFormat.format( debug, arguments );
				LAST_DEBUG_MESSAGE = logged;
				if ( arguments[lastArgument] instanceof Throwable && debug.indexOf( "{" + lastArgument + "}" ) == -1 ) {
					mLog.debug( logged, (Throwable) arguments[lastArgument] );
				} else {
					mLog.debug( logged );
				}

				if ( debug.indexOf( "{" + lastArgument + "}" ) == -1 ) {
					throw new RuntimeException( "Given " + ( lastArgument + 1 ) + " arguments to log, but no {" + lastArgument + "} in message '" + debug + "'" );
				}
			}
		}

		public void debug( String debug, Throwable throwable ) {

			LAST_DEBUG_MESSAGE = debug;

			mLog.debug( debug, throwable );
		}

		public boolean isInfoEnabled() {

			return mLog.isInfoEnabled();
		}

		public void info( String info, Object... arguments ) {

			int lastArgument = arguments.length - 1;

			if ( lastArgument == -1 ) {
				LAST_INFO_MESSAGE = info;
				mLog.info( info );
			} else {
				String logged = MessageFormat.format( info, arguments );
				LAST_INFO_MESSAGE = logged;
				if ( arguments[lastArgument] instanceof Throwable && info.indexOf( "{" + lastArgument + "}" ) == -1 ) {
					mLog.info( logged, (Throwable) arguments[lastArgument] );
				} else {
					mLog.info( logged );
				}

				if ( info.indexOf( "{" + lastArgument + "}" ) == -1 ) {
					throw new RuntimeException( "Given " + ( lastArgument + 1 ) + " arguments to log, but no {" + lastArgument + "} in message '" + info + "'" );
				}
			}
		}

		public void info( String info, Throwable throwable ) {

			LAST_INFO_MESSAGE = info;

			mLog.info( info, throwable );
		}

		public boolean isWarnEnabled() {

			return mLog.isWarnEnabled();
		}

		public void warn( String warning, Object... arguments ) {

			int lastArgument = arguments.length - 1;

			if ( lastArgument == -1 ) {
				LAST_WARN_MESSAGE = warning;
				mLog.warn( warning );
			} else {
				String logged = MessageFormat.format( warning, arguments );
				LAST_WARN_MESSAGE = logged;
				if ( arguments[lastArgument] instanceof Throwable && warning.indexOf( "{" + lastArgument + "}" ) == -1 ) {
					mLog.warn( logged, (Throwable) arguments[lastArgument] );
				} else {
					mLog.warn( logged );
				}

				if ( warning.indexOf( "{" + lastArgument + "}" ) == -1 ) {
					throw new RuntimeException( "Given " + ( lastArgument + 1 ) + " arguments to log, but no {" + lastArgument + "} in message '" + warning + "'" );
				}
			}
		}

		public void warn( String warning, Throwable throwable ) {

			LAST_WARN_MESSAGE = warning;

			mLog.warn( warning, throwable );
		}

		public boolean isErrorEnabled() {

			return mLog.isErrorEnabled();
		}

		public void error( String error, Object... arguments ) {

			int lastArgument = arguments.length - 1;

			if ( lastArgument == -1 ) {
				mLog.error( error );
			} else {
				String logged = MessageFormat.format( error, arguments );
				if ( arguments[lastArgument] instanceof Throwable && error.indexOf( "{" + lastArgument + "}" ) == -1 ) {
					mLog.error( logged, (Throwable) arguments[lastArgument] );
				} else {
					mLog.error( logged );
				}

				if ( error.indexOf( "{" + lastArgument + "}" ) == -1 ) {
					throw new RuntimeException( "Given " + ( lastArgument + 1 ) + " arguments to log, but no {" + lastArgument + "} in message '" + error + "'" );
				}
			}
		}

		public void error( String error, Throwable throwable ) {

			mLog.error( error, throwable );
		}
	}

	//
	// Private constructor
	//

	private LogUtils() {

		// Can never be called
	}
}
