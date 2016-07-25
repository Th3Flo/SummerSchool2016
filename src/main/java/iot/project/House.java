package iot.project;

public class House {
	private boolean[] lights;
	private boolean[] presence;
	
	public void toggle_light(int room){
		if(lights[room]){
			lights[room] = false;
		} else {
			lights[room] = true;
		}
	}
	
	public boolean light_status(int room){
		return lights[room];
	}
	
	public void set_presence(int room, boolean status){
		presence[room] = status;
	}
}
