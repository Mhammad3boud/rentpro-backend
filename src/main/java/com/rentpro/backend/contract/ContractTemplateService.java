package com.rentpro.backend.contract;

import com.rentpro.backend.user.User;
import com.rentpro.backend.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ContractTemplateService {

    private final ContractTemplateRepository templateRepository;
    private final UserRepository userRepository;

    // Default template content with placeholders
    public static final String DEFAULT_TEMPLATE_CONTENT = """
        RESIDENTIAL LEASE AGREEMENT
        
        Contract Number: {{CONTRACT_ID}}
        Date: {{GENERATED_DATE}}
        
        PARTIES:
        
        LANDLORD: {{LANDLORD_NAME}}
        Address: {{LANDLORD_ADDRESS}}
        
        TENANT: {{TENANT_NAME}}
        
        PROPERTY:
        {{PROPERTY_NAME}}
        {{PROPERTY_ADDRESS}}
        {{UNIT_NUMBER}}
        
        LEASE TERMS:
        
        1. TERM: This lease begins on {{START_DATE}} and ends on {{END_DATE}}.
        
        2. RENT: Tenant agrees to pay TZS {{MONTHLY_RENT}} per month, due on the 7th of each month.
        
        3. SECURITY DEPOSIT: Tenant has paid TZS {{SECURITY_DEPOSIT}} as security deposit.
        
        4. UTILITIES: {{UTILITIES_TERMS}}
        
        5. MAINTENANCE: Tenant shall maintain the premises in good condition and notify
           Landlord of any needed repairs promptly.
        
        6. PETS: {{PET_POLICY}}
        
        7. ADDITIONAL TERMS:
        {{SPECIAL_TERMS}}
        
        By signing below, both parties agree to the terms of this lease.
        
        ___________________________           ___________________________
        Landlord Signature                    Tenant Signature
        
        Date: ____________                    Date: ____________
        """;

    public ContractTemplateService(ContractTemplateRepository templateRepository,
                                   UserRepository userRepository) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
    }

    public List<ContractTemplate> getTemplatesForOwner(UUID ownerId) {
        List<ContractTemplate> templates = templateRepository.findByOwner_UserIdOrderByCreatedAtDesc(ownerId);
        
        // If no templates exist, create a default one
        if (templates.isEmpty()) {
            ContractTemplate defaultTemplate = createDefaultTemplate(ownerId);
            templates.add(defaultTemplate);
        }
        
        return templates;
    }

    public ContractTemplate getTemplateById(UUID templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    public ContractTemplate getDefaultTemplate(UUID ownerId) {
        return templateRepository.findByOwner_UserIdAndIsDefault(ownerId, true)
                .orElseGet(() -> createDefaultTemplate(ownerId));
    }

    @Transactional
    public ContractTemplate createTemplate(UUID ownerId, String name, String content) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ContractTemplate template = new ContractTemplate();
        template.setOwner(owner);
        template.setTemplateName(name);
        template.setTemplateContent(content);
        template.setDefault(false);
        
        return templateRepository.save(template);
    }

    @Transactional
    public ContractTemplate updateTemplate(UUID templateId, String name, String content) {
        ContractTemplate template = getTemplateById(templateId);
        template.setTemplateName(name);
        template.setTemplateContent(content);
        return templateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(UUID templateId) {
        ContractTemplate template = getTemplateById(templateId);
        if (template.isDefault()) {
            throw new RuntimeException("Cannot delete default template");
        }
        templateRepository.deleteById(templateId);
    }

    @Transactional
    public ContractTemplate setAsDefault(UUID ownerId, UUID templateId) {
        // Remove default from current default
        templateRepository.findByOwner_UserIdAndIsDefault(ownerId, true)
                .ifPresent(t -> {
                    t.setDefault(false);
                    templateRepository.save(t);
                });
        
        // Set new default
        ContractTemplate template = getTemplateById(templateId);
        template.setDefault(true);
        return templateRepository.save(template);
    }

    private ContractTemplate createDefaultTemplate(UUID ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        ContractTemplate template = new ContractTemplate();
        template.setOwner(owner);
        template.setTemplateName("Standard Lease Agreement");
        template.setTemplateContent(DEFAULT_TEMPLATE_CONTENT);
        template.setDefault(true);
        
        return templateRepository.save(template);
    }
}
