

public class WhatsappConnection {

	public Tree read_tree(DataBuffer data) {
		Tree t = new Tree("");
		return t;
	}
	
	/*int lsize = data->readListSize();
	int type = data->getInt(1);
	if (type == 1) {
		data->popData(1);
		Tree t;
		t.readAttributes(data,lsize);
		t.setTag("start");
		return t;
	}else if (type == 2) {
		data->popData(1);
		return Tree("treeerr"); // No data in this tree...
	}
	
	Tree t;
	t.setTag(data->readString());
	t.readAttributes(data,lsize);
	
	if ((lsize & 1) == 1) {
		return t;
	}
	
	if (data->isList()) {
		t.setChildren(data->readList(this));
	}else{
		t.setData(data->readString());
	}
	
	return t;*/

}


