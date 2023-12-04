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

/**@author endryys*/

public class Battery extends Agent {
    
 //Battery's attribute in order to send message mainly and interpret data
    
    private String p_nominal;
    private String capacity;
    private String soc;
    private Boolean charge,discharge;
    private String p_batt_output; 
    private String acD;
    private String batt_id_;
    
    private BatteryAction batt_attr;
    
    
    public float p_nominal_gui;
    public float capacity_gui;
    public float soc_gui;
    public Battery_GUI gui;
    
    //Arrays responsible for storing battery data
    
    /*public float[] a_p_batt_i=new float[48];
    public float[] a_soc_i=new float[48*/
    
    public String[] a_p_batt_i=new String[48];
    public String[] a_soc_i=new String[48];
    private int a=0;
    
    
    //Variables to substitute for decimals "." por ","
    
    private String p_bateria_Str;
    //private String pbatt;
    
    private String soc_Str;
    //private String soc_a;
   
    /*Attribute that must determine the seconds every when it sends a demand point, it is defined in 1800s
     Although this value must be taken from a textBox filled in by the user in a form*/
     public float ti=1800; 
        
    protected void setup(){
        
        System.out.println("Welcome to Battery Agent "+this.getName()+" .");
        gui=new Battery_GUI(this);
        gui.showGui();
        batt_attr=new BatteryAction();

    }
    protected void takeDown(){
    // Deregister from the yellow pages
	try {
            DFService.deregister(this);
        }
	catch (FIPAException fe) {
            fe.printStackTrace();
	}

	System.out.println("Agent Battery "+getAID().getName()+" terminated.\n\n");
    }
    
    public void Battery_Inialize(){
        
        String p_nominal_Str;
        float p_nominal_;
        float capacity_;
        float soc_;
        String b_id;
        String[] b_id_part;
        
        //Send values GUI to Battery Action
        batt_attr.CatchBatteryData(this.capacity_gui, this.p_nominal_gui, this.soc_gui);
        
       
        //BatteryAction batt_attr=new BatteryAction();
        p_nominal_=batt_attr.p_nominal_;
        capacity_=batt_attr.capacity_;
        soc_=batt_attr.soc_;
       
        DecimalFormat p1_generada_df = new DecimalFormat("0.00"); 
        p_nominal_Str=p1_generada_df.format(p_nominal_);  
        p_nominal=p_nominal_Str.replaceAll(",", ".");
        
      
        capacity=String.valueOf(capacity_);
        //The same conversion as above but in a different way
        soc=Float.toString(soc_);
        
        // Register the battery service in the yellow pages
        DFAgentDescription dfd_batt = new DFAgentDescription();
	    dfd_batt.setName(getAID());
	    ServiceDescription sd_batt = new ServiceDescription();
	    sd_batt.setType("pcc-baterias");
	    sd_batt.setName("Cartera de inversión 2 JADE");
	    dfd_batt.addServices(sd_batt);
	    try {
            DFService.register(this, dfd_batt);
	    }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
            
        System.out.println("Battery Agent "+getAID().getName()+" has the characteristics:");
        System.out.println("Pnominal: "+p_nominal+"(kW) "+"inserted in DF.");
        System.out.println("Capacity: "+capacity+"(kWh) "+"inserted in DF\n.");
        System.out.println("SOC: "+soc+"(%)"+" inserted in DF\nF.\n");
        
		// Add the behaviour serving queries from Power Manager agent
		addBehaviour(new DemandInfo_Server());

		// Add the behaviour serving purchase orders from Power Manager agent
		addBehaviour(new OrderAction_Server());
                
                //Add the behaviour for receive stop order
                addBehaviour(new Stop_Action());
                
                //Add the behaviour for finish software
                addBehaviour(new FinishMessage());
       
    }
    
    private class DemandInfo_Server extends CyclicBehaviour{
        
        public void action(){
                String b_id=new String();
                b_id=myAgent.getName();
                
                String[] b_id_part=b_id.split("@");
                batt_id_=b_id_part[0];
                
                
                
                String batt_input;
                float p_diff,status_,threshold_;
                float soc_=0;
                String batt_name;
                float[] batt_info_=new float[2];
                String p_batt_info,soc_info,aCD_info;
               //It's prepared to receive the message with structure ACCEPT_PROPORSAL
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                ACLMessage msg = myAgent.receive(mt);
                
                if(msg!=null){
                   // CFP Message received. Process it
                    batt_input = msg.getContent();
                    System.out.println("The battery\n "+getAID().getName()+" has received CFP from the PM with data of: "+batt_input );
                    
                    //It's convert the variable String to float
                    String arr =  batt_input;
                    String[] items = arr.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
                    float[] results = new float[items.length];

                    for (int i = 0; i < items.length; i++) {
                        try {                   
                            results[i]=Float.parseFloat(items[i]);
                        } catch (NumberFormatException nfe) {
                         //NOTE: write something here if you need to recover from formatting errors
                        }
                    }                    
                  
                    p_diff=results[0];
                    status_=results[1];
                    threshold_=results[2]; 

                    //PRUEBA UNITARIA
                    if(p_diff==221){
                        p_diff=221;
                    }
                    
                     // Set parameters to the information consume or supply and soc
                     //BatteryAction cfp_batteries=new BatteryAction();
                     
                     float p_nominal_=Float.parseFloat(p_nominal);
                     soc_=Float.valueOf(soc);
                     batt_name=myAgent.getAID().getName();
                    
                     //Function "BATTERY_INFORMATION" that determines parameters p_batt_max, soc_info, aCD_info
                     /*p_batt_max, is the maximum power that the battery can offer according to demand
                     that is, the maximum power it can offer is the p_diff demanded by PM, if not
                     could reach that value the p_batt_max = p_nominal and if the battery is found
                     in a SOC so minimal that it couldn't reach neither p_diff nor p_nominal it will deliver
                     that energy that his soc allows him to reach the soc_min, the same would happen to
                     time to consume, but with soc_max*/
                     
                     //batt_info_=cfp_batteries.Battery_Information(p_diff, status_, threshold_, soc_, batt_name,ti);
                     batt_info_=batt_attr.Battery_Information(p_diff, status_, threshold_, soc_, batt_name,ti);
                     
                     p_batt_info=Float.toString(batt_info_[0]);
                     soc_info=Float.toString(batt_info_[1]);
                     aCD_info=Float.toString(batt_info_[2]);

                     String batt_propose="["+p_batt_info+","+soc_info+","+aCD_info+"]";
                     ACLMessage reply_cfp = msg.createReply();
                     reply_cfp.setConversationId("PM_BATT");
                     reply_cfp.setPerformative(ACLMessage.PROPOSE);
                     reply_cfp.setContent(String.valueOf(batt_propose));
                     System.out.println("Battery " +batt_name+"send the proposal\n : "+batt_propose+"\n");
                     myAgent.send(reply_cfp); 
                      
                     //myAgent.doWait(1000);
                }
        }
    }    
    
    //Implementation fo behaviours
    private class OrderAction_Server extends CyclicBehaviour{
    
            public void action(){
                
                String batt_input;
                float p_diff,status_,threshold_;
                int status=0,threshold=0;
                float soc_max,soc_min,soc_upper,soc_lower,soc_current;
                float ti=1800; //The time interval always is 30 minutes
                float p_abs,p_deliver,p_charge,p_discharge;
                float soc_,soc_output;
                float p_batt_output_=0;
                float p_nominal_,p_batt_max;
                float capacity_;
                float aCD;
                
                aCD=0;
                capacity_=Float.parseFloat(capacity);
                p_nominal_=Float.parseFloat(p_nominal); 
                soc_=Float.valueOf(soc);
                soc_min=5;
                soc_max=98;
                soc_upper=40;
                soc_lower=30;
                soc_output=0;
                
                //It's prepared to receive the message with structure CFP
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                ACLMessage msg = myAgent.receive(mt); 
                
                if (msg != null) {
                    
                    if(a==28){
                    a=28;
                }
                    
                   // CFP Message received. Process it
                    batt_input = msg.getContent();
                    System.out.println("the Battery "+getAID().getName()+" has received ACCEPT PROPOSAL from the PM with data of: "+batt_input );
                    
                    //It's convert the variable String to float
                    String arr =  batt_input;
                    String[] items = arr.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
                    float[] results = new float[items.length];

                    for (int i = 0; i < items.length; i++) {
                        try {                   
                            results[i]=Float.parseFloat(items[i]);
                        } catch (NumberFormatException nfe) {
                         //NOTE: write something here if you need to recover from formatting errors
                        }
                    }                    
                    //String battery_data="["+p_generated+","+price+"]";
                    p_diff=results[0];
                    status_=results[1];
                    threshold_=results[2];
                    
                    //unit test
                    if(p_diff==221){
                        p_diff=221;
                    }
                    
                    
                    //if(p_diff<0){p_diff=-p_diff;}
                    if(status_==1.0){status=1;}
                    if(status_==2.0){status=2;}
                    if(threshold_==-1.0){threshold=-1;}
                    if(threshold_==0.0){threshold=0;} 
                    if(threshold_==1.0){threshold=1;} 
                    //Status 3 enters only when soc is not outside the limits of soc_lower and soc_upper
                    //and when the pcc_initial is within the limits of the PeakShaving
                    if(status_==3.0){
                        status=3;
                        if (soc_<soc_lower){
                          
                            charge=true;
                            discharge=false;
                        }
                        if(soc_>soc_upper){
                           
                            discharge=true;
                            charge=false;
                        }
                        
                            
                        if(soc_>=soc_lower && soc_<=soc_upper){
                               
                           charge=false;
                           discharge=false;
                        }
                    }
                    System.out.println("The float results are: p_diff= "+p_diff+"  status= "+status);
                    
                    
                    //BatteryAction process=new BatteryAction();
                    
                    switch(status){
                        
                        case 1://Battery deliver energy
                            
                            //The maximum power it can deliver is calculated
                            p_batt_max=batt_attr.PowerDischargingTillSOCmin(ti, soc_);
                            //p_batt_max=process.PowerDischargingTillSOCmin(ti, soc_);
                            
                            System.out.println("The Battery "+myAgent.getName()+" enters the energy delivery phase\n");
                            
                            if(soc_<=soc_min){
                                
                               System.out.println("The Battery "+myAgent.getName()+" is too low, cannot deliver power.");
                               p_batt_output_=0;
                               soc_output=soc_;
                               p_batt_output="0";
                               soc=Float.toString(soc_output);
                                
                            }else{

                                p_diff=-p_diff;
//                                if(p_diff>=p_nominal_){
//
//                                    p_batt_output_=p_nominal_;
//                                    //soc_output=process.DischargingStatus(p_batt_output_,ti,soc_);
//                                     soc_output=batt_attr.DischargingStatus(p_batt_output_,ti,soc_);
//                                    if(soc_output<1.03*soc_min){
//                                        soc_output=soc_min;
//                                       // p_batt_output_=process.PowerDischargingTillSOCmin( ti, soc_);
//                                        p_batt_output_=batt_attr.PowerDischargingTillSOCmin( ti, soc_);
//                                    }
//
//                                }else{
                                    p_batt_output_=p_diff;
                                    //soc_output=process.DischargingStatus(p_batt_output_,ti,soc_);
                                    soc_output=batt_attr.DischargingStatus(p_batt_output_,ti,soc_);
                                    if(soc_output<1.03*soc_min){
                                        soc_output=soc_min;
                                        //p_batt_output_=process.PowerDischargingTillSOCmin( ti, soc_);
                                        p_batt_output_=batt_attr.PowerDischargingTillSOCmin( ti, soc_);
                                    }
                                }
                            //}
                            
                            break;
                            
                        case 2://Battery consume energy
                            
                            System.out.println("The Battery "+myAgent.getName()+" enters energy consumption phase");
                            
                            if(soc_>=soc_max){
                               System.out.println("The Battery "+myAgent.getName()+" has too high a level, cannot absorb more energy.");
                               p_batt_output_=0;
                               soc_output=soc_;
                                p_batt_output="0";
                                soc=Float.toString(soc_output);
                                
                            }else{
                                //p_diff=-p_diff;
//                                if(p_diff>=p_nominal_){
//                                    p_batt_output_=p_nominal_;
//                                    //soc_output=process.ChargingStatus(p_batt_output_,ti,soc_);
//                                    soc_output=batt_attr.ChargingStatus(p_batt_output_,ti,soc_);
//                                    if(soc_output>1.03*soc_max){
//                                        soc_output=soc_max;
//                                        //p_batt_output_=process.PowerChargingTillSOCmax(ti,soc_);
//                                        p_batt_output_=batt_attr.PowerChargingTillSOCmax(ti,soc_);
//                                    }
//                                        
//                                }else{
                                    p_batt_output_=p_diff;
                                    //soc_output=process.ChargingStatus(p_batt_output_,ti, soc_);
                                    soc_output=batt_attr.ChargingStatus(p_batt_output_,ti, soc_);
                                    if(soc_output>1.03*soc_max){
                                        soc_output=soc_max;
                                        //p_batt_output_=process.PowerChargingTillSOCmax(ti,soc_);
                                        p_batt_output_=batt_attr.PowerChargingTillSOCmax(ti,soc_);
                                    }                                   
                                }
                           // }
                            
                            break;
                            
                        case 3://Battery automatic charge/discharge
                            
                            if(charge==true){
                                System.out.println("The Battery "+myAgent.getName()+" enters the automatic loading phase");
                                if(threshold==0 || threshold==-1){
                                    
                                    p_charge=(p_nominal_+p_nominal_*((soc_lower-soc_)/(soc_lower-soc_min)))/2;
                                    if(p_charge<=p_diff){
                                        //soc_output=process.ChargingStatus(p_charge,ti,soc_);
                                        soc_output=batt_attr.ChargingStatus(p_charge,ti,soc_);
                                        aCD=1;
                                        if(p_charge<0){p_charge=-p_charge;}    
                                            p_batt_output_=p_charge;
                                            System.out.println(myAgent.getName()+" absorbs a power\n: "+p_batt_output_ +" and stays in SOC: "+soc_output);
                                            //P_batt_output is left at zero, since in this case it should not consume power from the system since it is within the limits
                                            //p_batt_output_=0;
                                    }else{
                                        p_charge=p_diff;
                                        aCD=1;
                                        if(p_charge<0){p_charge=-p_charge;}
                                            //soc_output=process.ChargingStatus(p_charge,ti, soc_);
                                            soc_output=batt_attr.ChargingStatus(p_charge,ti, soc_);
                                            p_batt_output_=p_charge;
                                            System.out.println(myAgent.getName()+" absorbs a power: "+p_batt_output_ +" and stays in SOC: "+soc_output);
                                    }
                                }else{
                                    System.out.println("Paralyzed the process of automatic loading of "+myAgent.getName()+" for having reached the upper limit.\n");
                                    soc_output=soc_;
                                    p_batt_output_=0;
                                    aCD=0;
                                }
                            }
                            if(discharge==true){
                                System.out.println("The Battery "+myAgent.getName()+"enters the automatic discharge phase\n");
                                if(threshold==0 || threshold==1){
                                    p_discharge=(p_nominal_+p_nominal_*((soc_-soc_upper)/(soc_max-soc_upper)))/2;
                                    if(p_discharge<=p_diff){
                                    aCD=-1;
                                    if(p_discharge<0){p_discharge=-p_discharge;} 
                                        //soc_output=process.DischargingStatus(p_discharge,ti,soc_);
                                        soc_output=batt_attr.DischargingStatus(p_discharge,ti,soc_);
                                        p_batt_output_=p_discharge;
                                        System.out.println(myAgent.getName()+" yields a power: "+p_batt_output_ +" and stays in SOC\n: "+soc_output);
                                        //
                                        //P_batt_output is left at zero, since in this case it should not deliver system power since it is within the limits
                                        //p_batt_output_=0;
                                    }else{
                                        p_discharge=p_diff;              
                                        aCD=-1;    
                                        if(p_discharge<0){p_discharge=-p_discharge;} 
                                        //soc_output=process.DischargingStatus(p_discharge,ti,soc_);
                                        soc_output=batt_attr.DischargingStatus(p_discharge,ti,soc_);
                                        p_batt_output_=p_discharge;
                                        System.out.println(myAgent.getName()+" yields a power: "+p_batt_output_ +" and stays in SOC\n: "+soc_output);
                                        
                                    }
                                }else{
                                    System.out.println("Paralyzed the automatic download process of "+myAgent.getName()+" for having reached a lower limit.\n");
                                    soc_output=soc_;
                                    p_batt_output_=0;
                                    aCD=0;
                                }                                
                            }
                            //Efficient loading and unloading phase
                            /*This phase enters when the current SOC of the battery is within the established ranges, which does not have the need for automatic charge / discharge.
                            and can contribute or consume from the system to further dampen the Pcc_final curve.*/
                            if(charge==false && discharge==false){
                                System.out.println("The Battery "+myAgent.getName()+" stopped, proper SOC\n.");
                                p_batt_output_=0;
                                soc_output=soc_;
                                aCD=0;                               
                            }
                        
                            break;
                                
                    }
                     p_batt_output=Float.toString(p_batt_output_);
                     soc=Float.toString(soc_output);
                     acD=Float.toString(aCD);
                    
                    // Set parameters to the propose
                     String action="["+p_batt_output+","+soc+","+acD+"]";
                     ACLMessage reply_action = msg.createReply();
                     reply_action.setConversationId("PM_BAT_Action");
                     reply_action.setPerformative(ACLMessage.INFORM);
                     reply_action.setContent(String.valueOf(action));
                     myAgent.send(reply_action); 
                     System.out.println("The Battery "+myAgent.getName()+" send the action : "+action+"\n");
                     
                     if(a==28){
                        a=28;
                    }
                     
                    DecimalFormat p_bateria_df = new DecimalFormat("0.00");
                    //******ERROR***********************************************************************************
                     p_bateria_Str=p_bateria_df.format(Float.parseFloat(p_batt_output));  
                     
                     DecimalFormat soc_df = new DecimalFormat("0.00"); 
                     soc_Str=soc_df.format(Float.parseFloat(soc));  
                  
                 /*    //******************ARRAY P_BATT_i & SOC_i****************************************
                     a_p_batt_i[a]=p_batt_output_;
                     a_soc_i[a]=Float.parseFloat(soc);
                     a++;
                     //********************************************************************************/
                     
                     //******************ARRAY P_BATT_i & SOC_i****************************************
                     
                     if(p_bateria_Str==null){p_bateria_Str="0";}
                     if(soc_Str==null){soc_Str=soc_df.format(Float.parseFloat(soc));}
                     
                     a_p_batt_i[a]=p_bateria_Str;
                     a_soc_i[a]=soc_Str;
                     a++;
                     //*********************************************************************************/
                }
            }
    }
    
    private class Stop_Action extends CyclicBehaviour{
        
        private MessageTemplate mt_stop;
        private Boolean fin;
        
        public void action(){
            
            //mt_stop=MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL);
            
            mt_stop=MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL),MessageTemplate.MatchConversationId("STOP_BATT"));
            
            ACLMessage stop_order=receive(mt_stop);
            String soc_input;
            String soc_ws; //SOC without Stop
            String batt_id;
            if(stop_order!=null){
                
                 if(a==28){
                        a=28;
                    }
                     
                // STOP order message received. Process it
                    soc_input = stop_order.getContent();
                    batt_id=stop_order.getSender().getName();
                    
                    System.out.println("The Battery "+getAID().getName()+" has received the STOP command ");
                    
                    //It's convert the variable String to float
                    String arr =  soc_input;
                    //String[] items = arr.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
                    float results;
                    results=Float.parseFloat(soc_input);

                    /*for (int i = 0; i < items.length; i++) {
                        try {                   
                            results[i]=Float.parseFloat(items[i]);
                        } catch (NumberFormatException nfe) {
                         //NOTE: write something here if you need to recover from formatting errors
                        }
                    }                    
                    //String battery_data="["+p_generated+","+price+"]";
                    p_diff=results[0];
                    status_=results[1];
                    threshold_=results[2];*/
                    
                    //if(batt_id!=batt_id_) {
                        soc_ws=soc;
                        soc=soc_input;
                        System.out.println("Battery SOC is reset\n "+myAgent.getName()+"."+" of "+soc_ws+" a -----> "+soc);
                        batt_id_=batt_id;
                        
                        DecimalFormat soc_df = new DecimalFormat("0.00");
                        soc_Str=soc_df.format(Float.parseFloat(soc));
                        //String soc_a=soc_Str.replaceAll(",", ",");
                        
                       /* soc_a=soc_Str.replaceAll(",", ".");*/
                        
                     /*  // *****************ARRAY P_BATT_i & SOC_i*************************************
                        a_p_batt_i[a]=0;
                        a_soc_i[a]=Float.parseFloat(soc);
                        a++;
                     //********************************************************************************     */
                    //}
                    if(a==28){
                        a=28;
                    }
                    //******************ARRAY P_BATT_i & SOC_i*************************************
                    
                        if(p_bateria_Str==null){
                            p_bateria_Str="0";
                        }
                        if(soc_Str==null){
                            soc_Str=soc_df.format(Float.parseFloat(soc));
                        }
                        
                        a_p_batt_i[a]="0";
                        a_soc_i[a]=soc_Str;
                        a++;
                     //********************************************************************************
                    
               
            }else{
                
                block();
            }
        }
        
       /*  public boolean done() {
             fin=true;
             return fin;
         } */
    }

        private class FinishMessage extends CyclicBehaviour{
            
            private MessageTemplate mt; // The template to receive replies
           // ExportCSV filecsv=new ExportCSV();
            public void action() {
                mt=MessageTemplate.MatchPerformative(ACLMessage.CANCEL);
                ACLMessage finish=receive(mt);
                
                    if(finish!=null){
                        final String nombreDeArchivo = "/Users/sreeramvennapusa/IdeaProjects/MultiAgentJadeProject/src/org/MicroGridJade/_"+batt_id_+"_output.csv";
                      //  filecsv.CreatefileCSVBATT(nombreDeArchivo,";", a_p_batt_i, a_soc_i);
                        System.out.println();
                        myAgent.doDelete();
                    }    
            }
        }     
}