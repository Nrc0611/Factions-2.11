package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.util.Txt;

public class FactionsCommand extends MassiveCommand
{
	// -------------------------------------------- //
	// FIELDS
	// -------------------------------------------- //
	
	public MPlayer msender;
	public Faction msenderFaction;

	protected boolean settingUpStandard = true;
	public boolean isSettingUpStandard() { return this.settingUpStandard; }
	public void setSettingUpStandard(boolean settingUpStandard) { this.settingUpStandard = settingUpStandard; }
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void senderFields(boolean set)
	{
		this.msender = set ? MPlayer.get(sender) : null;
		this.msenderFaction = set ? this.msender.getFaction() : null;
	}

	@Override
	public void setParent(MassiveCommand parent)
	{
		if (this.isSettingUpStandard()) this.setupStandard(parent);
		super.setParent(parent);
	}

	public void setupStandard(MassiveCommand parent)
	{
		// Gather
		String name = this.getClass().getSimpleName();
		String nameParent = parent.getClass().getSimpleName();

		// Alias
		String alias = name.substring(nameParent.length());
		alias = alias.toLowerCase();
		this.getAliases().add(0, alias);

		Object permission = getPermission();
		if (permission != null) this.addRequirements(RequirementHasPerm.get(permission));
	}

	protected Object getPermission()
	{
		// Perm
		String permName = this.getClass().getSimpleName().substring("CmdFactions".length());
		permName = Txt.implode(Txt.camelsplit(permName), "_");
		permName = permName.toUpperCase();

		Perm perm;
		try
		{
			perm = Perm.valueOf(permName);
		}
		catch(IllegalArgumentException ex)
		{
			// Didn't work. Try wihtout underscore
			permName = permName.replace("_", "");
			perm = Perm.valueOf(permName);
		}

		return perm;
	}

}
