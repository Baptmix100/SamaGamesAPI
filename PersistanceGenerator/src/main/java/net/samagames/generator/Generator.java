package net.samagames.generator;

import com.squareup.javapoet.*;
import net.samagames.persistanceapi.beans.players.PlayerSettingsBean;
import net.samagames.persistanceapi.beans.shop.TransactionBean;
import net.samagames.persistanceapi.beans.statistics.PlayerStatisticsBean;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Silvanosky on 25/03/2016.
 */
public class Generator {

    private static List<JavaFile> toBuild = new ArrayList<>();

    public static void main(String[] args)
    {
        loadGameStats();

        build();
    }

    public static void loadGameStats()
    {
        // STATISTICS
        TypeSpec.Builder playerStatsBuilder = TypeSpec.interfaceBuilder("IPlayerStats")
                .addModifiers(Modifier.PUBLIC);

        playerStatsBuilder.addMethod(getMethod("updateStats", void.class));
        playerStatsBuilder.addMethod(getMethod("refreshStats", boolean.class));
        playerStatsBuilder.addMethod(getMethod("getPlayerUUID", UUID.class));

        String package_ = "net.samagames.api.stats";
        String package_game = package_ + ".games";

        Field[] playerStatisticFields = PlayerStatisticsBean.class.getDeclaredFields();
        for (Field field : playerStatisticFields)
        {
            field.setAccessible(true);
            TypeSpec statInterface = createInterfaceOfType(field.getType(), true);

            //Create getter in player stat
            playerStatsBuilder.addMethod(
                    getMethod("get" + statInterface.name.substring(1), ClassName.get(package_game, statInterface.name)));

            toBuild.add(JavaFile.builder(package_game, statInterface).build());
        }

        toBuild.add(JavaFile.builder(package_, playerStatsBuilder.build()).build());
        // END STATISTICS

        // SETTINGS
        TypeSpec playerSettingsBean = createInterfaceOfType(PlayerSettingsBean.class, false);
        toBuild.add(JavaFile.builder("net.samagames.api.settings", playerSettingsBean).build());
        // END SETTINGS

        // SHOP ITEM TransactionBean
        TypeSpec playerTransactionBean = createInterfaceOfType(TransactionBean.class, false);
        toBuild.add(JavaFile.builder("net.samagames.api.shops", playerTransactionBean).build());
        //END SHOP ITEM
    }

    public static void build()
    {
        try {
            File file = new File("./Generation");
            file.delete();
            for (JavaFile javaFile : toBuild)
            {
                javaFile.writeTo(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static MethodSpec getMethod(String name, TypeName retur)
    {
        MethodSpec.Builder getter = MethodSpec.methodBuilder(name);
        getter.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
        getter.returns(retur);
        return getter.build();
    }

    public static MethodSpec getMethod(String name, Type type)
    {
        return getMethod(name, TypeName.get(type));
    }

    public static TypeSpec createInterfaceOfType(Class type, boolean useIncrement)
    {
        String name = "I" +type.getSimpleName().replaceAll("Bean", "");

        TypeSpec.Builder object = TypeSpec.interfaceBuilder(name)
                .addModifiers(Modifier.PUBLIC);
        object.addMethod(getMethod("update", void.class));
        object.addMethod(getMethod("refresh", void.class));
        Method[] subDeclaredMethods = type.getDeclaredMethods();
        for (Method method : subDeclaredMethods)
        {
            String methodName = method.getName();
            if (method.getParameters().length > 0)
            {
                if (methodName.startsWith("set") && useIncrement)
                {
                    boolean isIncrementable = false;
                    Class<?> type1 = method.getParameters()[0].getType();
                    if (type1.equals(int.class)
                            || type1.equals(long.class)
                            || type1.equals(double.class)
                            || type1.equals(float.class))
                        isIncrementable = true;

                    if (isIncrementable)
                    {
                        methodName = "incrBy" + methodName.substring(3);
                    }
                }

            }

            MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName);
            if (method.getParameterCount() > 0)
            {
                for (Parameter parameter : method.getParameters())
                {
                    builder.addParameter(parameter.getType(), parameter.getName());
                }
            }
            builder.addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT);
            builder.returns(method.getReturnType());
            object.addMethod(builder.build());
        }
        return object.build();
    }
}
