#!/usr/bin/env python3
"""Runner-only hero scripts: override global Cold/Heat rules for the live test."""
from pathlib import Path

root = Path('src/main/resources/assets/fiskheroes/data/heroes')
for hero in ('captain_cold', 'heatwave'):
    path = root / (hero + '.js')
    text = path.read_text()
    needle = '    hero.setHasPermission(hasPermission);'
    if text.count(needle) != 1:
        raise RuntimeError(f'Could not patch rule callback for {hero}')
    callback = '''    hero.setRuleValueModifier(function(entity, rule) {
        if (rule.name() == "fiskheroes:range_coldgun" || rule.name() == "fiskheroes:range_heatgun")
            return rule.isFloat() && rule.defaultValue() == 32 && rule.value() == 32 ? 6 : null;
        if (rule.name() == "fiskheroes:dmg_coldgun")
            return rule.isFloat() && rule.defaultValue() == 7 && rule.value() == 7 ? 23 : null;
        if (rule.name() == "fiskheroes:dmg_heatgun")
            return rule.isFloat() && rule.defaultValue() == 5 && rule.value() == 5 ? 25 : null;
        if (rule.name() == "fiskheroes:cooldown_coldgun")
            return rule.isInteger() && rule.defaultValue() == 20 && rule.value() == 20 ? 200 : null;
        return rule.value();
    });
'''
    path.write_text(text.replace(needle, callback + needle))
