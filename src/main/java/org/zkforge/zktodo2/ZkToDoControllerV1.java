package org.zkforge.zktodo2;

import static java.lang.System.out;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Window;

/**
 * This class demonstrates "Passive View" pattern as the Composer is 
 * doing all the explicit work of updating the UI. 
 * 
 * {@link http://martinfowler.com/eaaDev/PassiveScreen.html}
 */
@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class ZkToDoControllerV1 extends SelectorComposer<Window> implements
		ListitemRenderer<Reminder> {

	private static final long serialVersionUID = -3486059156312322420L;

	@WireVariable ReminderService reminderService;

	@Wire Textbox name;
	@Wire Intbox priority;
	@Wire Datebox date;
	@Wire Listbox list;
	
	ListModelList<Reminder> listModelList;	
	Reminder selectedReminder = null;
	
	public ZkToDoControllerV1() {
		// no nothing
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void doAfterCompose(Window comp) throws Exception {
		super.doAfterCompose(comp);
		listModelList = new ListModelList();
		List<Reminder> reminders = reminderService.findAll();
		listModelList.addAll(reminders);
		list.setModel(listModelList);
		list.setItemRenderer(this);
		list.addEventListener("onSelect", new EventListener(){
			public void onEvent(Event e)
					throws Exception {
				int index = list.getSelectedIndex();
				selectedReminder = (Reminder) listModelList.get(index);
				date.setValue(selectedReminder.getDate());
				priority.setValue(selectedReminder.getPriority());
				name.setValue(selectedReminder.getName());
				return;
			}});
	}

	@Listen("onClick = #add")
	public void add(Event e) {
		Date dateValue = date.getValue();
		Integer priorityValue = priority.getValue();
		String nameValue = name.getValue();
		if( dateValue != null && priorityValue != null && nameValue != null ){
			Reminder reminder = new Reminder();
			reminder.setDate(date.getValue());
			reminder.setName(name.getValue());
			reminder.setPriority(priority.getValue());
			this.reminderService.persist(reminder);
			List<Reminder> reminders = this.reminderService.findAll();
			this.listModelList.clear();
			this.listModelList.addAll(reminders);
			this.selectedReminder = reminder;
		}
	}

	@Listen("onClick = #update")
	public void update(Event e) {
		if( selectedReminder != null ){
			selectedReminder.setDate(date.getValue());
			selectedReminder.setPriority(priority.getValue());
			selectedReminder.setName(name.getValue());
			try {
				this.reminderService.merge(selectedReminder);
			} catch (EntityNotFoundException exception){
				int index = list.getSelectedIndex();
				listModelList.remove(index);
				alert("Reminder "+selectedReminder.getName()+" has been deleted by another user.");
				if( listModelList.size() > 0 ){
					selectedReminder = (Reminder)listModelList.get(0);
					list.setSelectedIndex(0);
					name.setValue(selectedReminder.getName());
					date.setValue(selectedReminder.getDate());
					priority.setValue(selectedReminder.getPriority());
				} else {
					selectedReminder = null;
				}
			}
			List<Reminder> reminders = reminderService.findAll();
			listModelList.clear();
			listModelList.addAll(reminders);
			
		}
	}

	@Listen("onClick = #delete")
	public void delete(Event e) {
		if( null != selectedReminder ){
			int index = listModelList.indexOf(selectedReminder);
			try {
				this.reminderService.delete(selectedReminder);
			} catch (EntityNotFoundException exception ){
				out.println("This is harmless as someone else has already deleted this item.");
			}
			listModelList.remove(selectedReminder);
			if( index >= listModelList.size() ){
				index = listModelList.size() - 1;
			} 
			if( listModelList.size() > 0 ){
				selectedReminder = (Reminder)listModelList.get(index);
				list.setSelectedIndex(index);
				name.setValue(selectedReminder.getName());
				date.setValue(selectedReminder.getDate());
				priority.setValue(selectedReminder.getPriority());
			} else {
				selectedReminder = null;
			}
		}
	}

	protected SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");
	
	@Override
	public void render(Listitem listItem, Reminder reminder, int index) throws Exception {
		new Listcell(reminder.getName()).setParent(listItem);
		new Listcell(reminder.getPriority()+"").setParent(listItem);
		new Listcell(dateFormat.format(reminder.getDate())).setParent(listItem);
	}

}
