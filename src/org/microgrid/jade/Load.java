package org.microgrid.jade;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class Load extends Agent{


    private float[] demandValue;
    private boolean end;
    private AID[] powerManager;
    public int j=0;// Auxiliar variable that serve to instance DemandRequest Behavoiur or finishing the Agent
    private String[] demandValue_Str;

    /* Attribute that must determine the seconds every when it sends a demand point, it is defined in 1800s
        Although this value must be taken from a textBox filled in by the user in a form */
    public float ti=1800;

    protected void setup(){

        System.out.println("Welcome consumer "+this.getName()+".");

        demandValue=new float[24];
        demandValue_Str=new String[24];

        demandValue[0]=100;
        demandValue[1]=100;
        demandValue[2]=100;
        demandValue[3]=300;
        demandValue[4]=500;
        demandValue[5]=600;
        demandValue[6]=700;
        demandValue[7]=800;
        demandValue[8]=900;
        demandValue[9]=1000;
        demandValue[10]=600;
        demandValue[11]=900;
        demandValue[12]=900;
        demandValue[13]=900;
        demandValue[14]=900;
        demandValue[15]=900;
        demandValue[16]=900;
        demandValue[17]=900;
        demandValue[18]=900;
        demandValue[19]=800;
        demandValue[20]=900;
        demandValue[21]=900;
        demandValue[22]=300;
        demandValue[23]=200;

        //It's convert demandValue to String
        for(int i=0; i<demandValue.length;i++){
            demandValue_Str[i]=Float.toString(demandValue[i]);
        }


        //It's register in other to be localizated by Power Manager and receiving the confirmation the demand point
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("LoadRequest");
        sd.setName("Load Request JADE");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        }catch (FIPAException fe) {
            fe.printStackTrace();
        }


        addBehaviour(new SearchPM());


    }


    private class SearchPM extends Behaviour{

        private int count;
        DFAgentDescription[] result1=new DFAgentDescription[1];

        public void action(){

            //Search the PM
            DFAgentDescription template = new DFAgentDescription();//DF template to generators
            ServiceDescription sd = new ServiceDescription();  //Service description to generators
            sd.setType("controlAgent");
            template.addServices(sd);

            try {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                result1=result;
                if(result.length>0){

                    System.out.println("There are the following Control Agent Power Managers:");
                    powerManager = new AID[result.length];

                    for (int i = 0; i < result.length; ++i) {

                        powerManager[i] = result[i].getName();
                        System.out.println(powerManager[i].getName());
                    }
                    myAgent.addBehaviour(new DemandRequest());
                }else{
                    System.out.println("Waiting for Control Agent ... ");
                    //myAgent.doSuspend();
                    block();
                    myAgent.doWait(20000);
                  /*  count++;
                    if (count==8){
                      myAgent.doDelete();
                    }*/

                }
            }catch (FIPAException fe) {
                fe.printStackTrace();
            }


        }
        public boolean done(){
            end=true;
            return (result1.length>0);
        }
    }

    private class DemandRequest extends Behaviour{

        private AID pm_id[];
        private MessageTemplate mt;


        public void action(){

            // Define the type of message
            ACLMessage request= new ACLMessage(ACLMessage.REQUEST);

            for (int i = 0; i < powerManager.length; ++i) {
                request.addReceiver(powerManager[i]);
            }

            //It's convert demandValue to String
            //for(int i=0; i<demandValue.length;i++){
            //demandValue_Str[i]=Float.toString(demandValue[i]);
            //  }

            //It's sent the demanded power
            pm_id=new AID[powerManager.length];
            request.setContent(demandValue_Str[j]);
            request.setConversationId("D_PM");
            request.setReplyWith("request"+System.currentTimeMillis()); // Unique value
            System.out.println("The consumer "+myAgent.getName()+" send Request: "+request.getContent());
            myAgent.send(request);
            j++;

            if(j>0 && j<demandValue.length){
                addBehaviour(new PM_Commit());
            }
        }

        public boolean done(){
            end=true;
            return end;
        }
    }

    private class PM_Commit extends CyclicBehaviour{

        private MessageTemplate mt;

        public void action(){

            mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
            ACLMessage commit_demand=myAgent.receive(mt);

            if(commit_demand!=null){

                System.out.println("\n\n\n\nThe consumer "+myAgent.getName()+" has received confirmation "+commit_demand.getContent()+"\n\n\n\n");

                if(j<demandValue.length){

                    addBehaviour(new DemandRequest());
                }
                if(j>=demandValue.length){
                    // Define the type of message
                    ACLMessage finish= new ACLMessage(ACLMessage.CANCEL);

                    System.out.println("\nThe consumer "+myAgent.getName()+" is finished");

                    for (int i = 0; i < powerManager.length; ++i) {
                        finish.addReceiver(powerManager[i]);
                    }
                    finish.setContent("Last demand point");
                    finish.setConversationId("Finish process");
                    finish.setReplyWith("Finish"+System.currentTimeMillis());
                    System.out.println("The consumer send finish order to Power Manager \n");
                    myAgent.send(finish);
                    doDelete();
                }

            }else{
                block();
            }
        }
    }
}

