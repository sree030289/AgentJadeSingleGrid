package org.microgrid.jade;

/**@author endryys*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class Battery_GUI extends JFrame {

    //protected ArrayList<LoadMicroGrid2> consumers = new ArrayList();
    private Battery battery;
    private JTextField capacity;
    private JTextField nominal_p;
    private JTextField soc;

    Battery_GUI(Battery batt){
        super(batt.getLocalName());
       
       
        
        battery=batt;
        
        JPanel p = new JPanel();
        p.setLayout(new GridLayout(3, 2));
	p.add(new JLabel("Capacity (Wh) :"));
        capacity=new JTextField(20);
        p.add(capacity);
       // p.add(new JLabel("Nominal power (W) :"));
        //nominal_p=new JTextField(20);
        //p.add(nominal_p);
        p.add(new JLabel("Initial SOC (%) :"));
        soc=new JTextField(20);
        p.add(soc);
        getContentPane().add(p, BorderLayout.CENTER);
       
        
        JButton addButton = new JButton("Enter data");
		addButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
                                    
                                    String capacity_=capacity.getText().trim();
                                    //String nominal_p_=nominal_p.getText().trim();
                                    String soc_=soc.getText().trim();
                                    battery.capacity_gui=Float.parseFloat(capacity_);
                                   // battery.p_nominal_gui=Float.parseFloat(nominal_p_);
                                    battery.soc_gui=Float.parseFloat(soc_);
                                    battery.Battery_Inialize();
                                    dispose();
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(Battery_GUI.this, "Invalid Value. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		} );
		p = new JPanel();
                
                p.setLayout(new GridLayout(2, 2));
		p.add(addButton);
                
                getContentPane().add(p, BorderLayout.SOUTH);
                setResizable(false);
    }

    public static void main(String [] args)
    {
        new Battery_GUI(new Battery()).showGui();
    }
    
    public void showGui() {
        
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
                setSize(300,500);
		super.setVisible(true);
	}
    
}
