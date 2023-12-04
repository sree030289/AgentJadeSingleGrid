
package org.microgrid.jade;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.text.DecimalFormat;

//Library to round decimals

/**
 *
 * @author sreeramvennapusa
 */
public class WindGenerator extends Agent{
        String p_generated;
      //  String price;
        String p1_generated;
        //String price1;
	private float[] windGeneratorValue;
	private String[] windGeneratorValue_Str;
	Load load= new Load();
	String p1_generated_Str;
	double p1_generated_;
	int j=0;
	// Put agent initializations here
	protected void setup() {


		// Create the catalogue
		windGeneratorValue=new float[30];
		windGeneratorValue_Str=new String[30];

windGeneratorValue[0]=100;
windGeneratorValue[1]=200;
windGeneratorValue[2]=300;
windGeneratorValue[3]=120;
windGeneratorValue[4]=50;
windGeneratorValue[5]=60;
windGeneratorValue[6]=70;
windGeneratorValue[7]=120;
windGeneratorValue[8]=200;
windGeneratorValue[9]=140;
windGeneratorValue[10]=500;
windGeneratorValue[11]=550;
windGeneratorValue[12]=600;
windGeneratorValue[13]=620;
windGeneratorValue[14]=580;
windGeneratorValue[15]=480;
windGeneratorValue[16]=320;
windGeneratorValue[17]=100;
windGeneratorValue[18]=140;
windGeneratorValue[19]=130;
windGeneratorValue[20]=180;
windGeneratorValue[21]=140;
windGeneratorValue[22]=120;
windGeneratorValue[23]=130;



		//It's convert demandValue to String
		for(int i=0; i<windGeneratorValue.length;i++){
			windGeneratorValue_Str[i]=Float.toString(windGeneratorValue[i]);
		}

                System.out.println("Welcome! Agent-Generator "+getAID().getName()+" It is read.");

                

		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("demand-generation");
		sd.setName("demand-generation JADE");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
                System.out.println("Wind MicroGrid1 Agent-Generator "+getAID().getName()+" delivery:");
               // System.out.println(p1_generated+"(kW) "+"inserted in DF. Price = "+price1+"\n");

		// Add the behaviour serving queries from Power Manager agent
		addBehaviour(new DemandOfferServer());

		// Add the behaviour serving purchase orders from Power Manager agent
		addBehaviour(new OrdersPurchaseServer());
                
                // Add the behaviour serving purchase orders from Power Manager agent
		addBehaviour(new FinishMessage());
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		System.out.println("Agent-Generator "+getAID().getName()+" terminated.\n\n");
	}

	private class DemandOfferServer extends CyclicBehaviour {
            
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {

				// CFP Message received. Process it
				ACLMessage respuesta = msg.createReply();

				p1_generated_= Double.parseDouble(windGeneratorValue_Str[j]);
				DecimalFormat p1_generada_df = new DecimalFormat("0.00");
				p1_generated_Str=p1_generada_df.format(p1_generated_);
				p1_generated=p1_generated_Str.replaceAll(",", ".");

				System.out.println("Power from wind MicroGrid1 Generator: "+ p1_generated);
				p_generated=p1_generated;
               // price=price1;
                                        
				if (p_generated != null) {
					String propuesta="["+p_generated+"]";
					respuesta.setPerformative(ACLMessage.PROPOSE);
					respuesta.setContent(String.valueOf(propuesta));
				}
				else {
					respuesta.setPerformative(ACLMessage.REFUSE);
					respuesta.setContent("No Disponible");
				}
				myAgent.send(respuesta);
				j++;
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer

	/**
	   Inner class PurchaseOrdersServer.
	   This is the behaviour used by Book-seller agents to serve incoming 
	   offer acceptances (i.e. purchase orders) from buyer agents.
	   The seller agent removes the purchased book from its catalogue 
	   and replies with an INFORM message to notify the buyer that the
	   purchase has been sucesfully completed.
	 */
	private class OrdersPurchaseServer extends CyclicBehaviour {
            
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
                        
			if (msg != null) {
				// ACCEPT_PROPOSAL Message received. Process it
                                System.out.println("\nWind MicroGrid1 Generator "+myAgent.getName()+" has received ACCEPT_PROPORSAL.");
                                System.out.println("\nSupply in progress ...");
				String power = msg.getContent();
				ACLMessage response = msg.createReply();

				//Integer price = (Integer) catalogo.remove(titulo);
				if (p_generated != null) {
					response.setPerformative(ACLMessage.INFORM);
                                        response.setContent(p_generated);
					System.out.println(myAgent.getName()+" supply "+p_generated+"(kW) a agent "+msg.getSender().getName());
				}
				else {
					// The requested book has been sold to another buyer in the meanwhile .
					response.setPerformative(ACLMessage.FAILURE);
					response.setContent("no-disponible");
				}
                                myAgent.send(response);
                                if (response.getPerformative() == ACLMessage.INFORM){
                                    //myAgent.doDelete();
                                    System.out.println("Wind MicroGrid1 Generator "+myAgent.getName()+" demand point has ended.\n");
                                }
                                if (response.getPerformative() == ACLMessage.FAILURE){
                                    System.out.println(power+"Has NOT been supplied to agent "+msg.getSender().getName());
                                }
                                
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer    
        
        private class FinishMessage extends CyclicBehaviour{
            
            private MessageTemplate mt; // The template to receive replies
       
            public void action() {
                mt=MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
                ACLMessage finish=receive(mt);
                
                    if(finish!=null){
                        System.out.println();
                        myAgent.doDelete();
                    }    
            }
        }      
    
}
