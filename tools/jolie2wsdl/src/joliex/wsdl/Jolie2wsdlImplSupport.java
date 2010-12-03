/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package joliex.wsdl;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;

/**
 *
 * @author Francesco
 */
public class Jolie2wsdlImplSupport
{
	public static void main( String[] args )
	{
		String args0 = "./provaInputPorts.ol";
		String args1 = "./provaInputPortsImplSupp.wsdl";
		String[] s = {args0, args1};
		try {
			/*
			CommandLineParser cmdParser = new CommandLineParser( args, Jolie2wsdlImplSupport.class.getClassLoader() );
			args = cmdParser.arguments();
			OLParser parser = new OLParser(
			new Scanner( cmdParser.programStream(), cmdParser.programFilepath() ),
			cmdParser.includePaths(),
			Jolie2wsdlImplSupport.class.getClassLoader() );
			Program program = parser.parse();
			WSDLDocumentCreator document = new WSDLDocumentCreator();
			document.createDocument( program, cmdParser.programFilepath() );
			 */
			InputStream olStream = new FileInputStream( args0 );
			OLParser olParser = new OLParser(
				new Scanner( olStream, args0 ),
				new String[]{"."},
				Thread.currentThread().getContextClassLoader() );
			Program program = olParser.parse();
			//System.out.println(" program="+program );
			SemanticVerifier semantic = new SemanticVerifier( program );
			if ( semantic.validate() ) {
				ProgramVisitor programVisitor = new ProgramVisitor( program );
				programVisitor.run();
				WSDLDocumentCreatorImplTree document = new WSDLDocumentCreatorImplTree( programVisitor );
				document.ConvertDocument();
			}
		} //			catch( CommandLineException e ) {
		//			System.out.println( e.getMessage() );
		//			System.out.println( "Syntax is: jolie2wsdl [jolie options] <jolie filename> <output filename>" );
		//		}
		catch( IOException e ) {
			e.printStackTrace();
		} catch( ParserException e ) {
			System.out.println( e.getMessage() );
		}
//		catch( DocumentCreationException e ) {
//			e.printStackTrace();
//		}

	}
}
