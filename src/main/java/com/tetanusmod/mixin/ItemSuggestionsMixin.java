package com.tetanusmod.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.*;
import com.tetanusmod.TetanusMod;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Mixin(ItemStackArgumentType.class)
public abstract class ItemSuggestionsMixin {
    @Inject(method="listSuggestions",at=@At("RETURN"),cancellable=true)
    private <S> void modItems(CommandContext<S> context,SuggestionsBuilder builder,CallbackInfoReturnable<CompletableFuture<Suggestions>> result){
        String text=builder.getRemaining().toLowerCase(Locale.ROOT);
        if(text.indexOf('{')>=0||text.indexOf('[')>=0)return;
        var extra=new SuggestionsBuilder(builder.getInput(),builder.getStart());
        for(var id:Registries.ITEM.getIds())if(id.getNamespace().equals(TetanusMod.MOD_ID)&&(id.getPath().startsWith(text)||id.toString().startsWith(text)||text.startsWith("rustandruin:")&&id.getPath().startsWith(text.substring(12))))extra.suggest(id.toString());
        result.setReturnValue(result.getReturnValue().thenCombine(extra.buildFuture(),(original,added)->Suggestions.merge(context.getInput(),List.of(original,added))));
    }
}